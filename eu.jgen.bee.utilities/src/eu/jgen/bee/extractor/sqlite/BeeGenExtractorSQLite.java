/**
 * Copyright Marek Stankiewicz, 2020
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package eu.jgen.bee.extractor.sqlite;

import java.io.FileNotFoundException;
import java.util.List;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;

import java.sql.*;

import com.ca.gen.jmmi.Ency;
import com.ca.gen.jmmi.EncyManager;
import com.ca.gen.jmmi.MMObj;
import com.ca.gen.jmmi.Model;
import com.ca.gen.jmmi.ModelManager;
import com.ca.gen.jmmi.exceptions.EncyException;
import com.ca.gen.jmmi.exceptions.EncyUnsupportedOperationException;
import com.ca.gen.jmmi.exceptions.ModelNotFoundException;
import com.ca.gen.jmmi.ids.ObjId;
import com.ca.gen.jmmi.schema.AscTypeCode;
import com.ca.gen.jmmi.schema.AscTypeHelper;
import com.ca.gen.jmmi.schema.ObjTypeHelper;
import com.ca.gen.jmmi.schema.PrpTypeCode;
import com.ca.gen.jmmi.schema.PrpTypeHelper;
import com.ca.gen.jmmi.util.PrpFormat;

public class BeeGenExtractorSQLite {

	private Connection connection = null;
	private Model model;
	private Ency ency;
	private int objectcount;
	private int propertycount;
	private int associationcount;

	public static void main(String[] args) {

		System.out.println("Bee Gen Model Creator for SQLite, Version 0.1.");
		BeeGenExtractorSQLite extractor = new BeeGenExtractorSQLite();
		try {
			extractor.usage();
			extractor.start(args[0], args[1]);
			System.out.println("Extraction completed.");
		} catch (EncyException e) {
			System.out.println("Problem with connecting to the encyclopedia.");
			e.printStackTrace();
		} catch (ModelNotFoundException e) {
			System.out.println("Cannot find model oin the encyclopedia.");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("Problem with creating SQLIte database.");
			e.printStackTrace();
		}
	}

	private void usage() {
		System.out.println("USAGE:");
		System.out.println(
				"\tpathModel      -   Location of the directory containing CA Gen Local Model (directory ending with .ief)");
		System.out.println("\tpathOutput      -  Location of the directory to store generated SQLLite database ");
		System.out.println("\n");
	}

	private void start(String modelPath, String outputPath)
			throws EncyException, ModelNotFoundException, FileNotFoundException {
		ency = EncyManager.connectLocalForReadOnly(modelPath);
		model = ModelManager.open(ency, ency.getModelIds().get(0));

		createDatabase(outputPath);

		System.out.println("\tNumber of exported objects is " + objectcount);
		System.out.println("\tNumber of exported properties is " + propertycount);
		System.out.println("\tNumber of exported associations is " + associationcount);
	}

	private void createDatabase(String outputPath) {

		try {
			Class.forName("org.sqlite.JDBC");
			final SQLiteConfig config = new SQLiteConfig();
			config.setJournalMode( JournalMode.OFF);
			connection = config.createConnection("jdbc:sqlite:" + outputPath + "\\model.db");

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Opened database successfully");

		String droptbl1 = "DROP TABLE IF EXISTS  GenObjects;";
		String droptbl2 = "DROP TABLE  IF EXISTS GenAssociations;";
		String droptbl3 = "DROP TABLE  IF EXISTS GenProperties;";

		String sqlTblObj = "CREATE TABLE  GenObjects (\n" + "	id INTEGER PRIMARY KEY,\n"
				+ "  objType  INTEGER NOT NULL,\n" + "  objMnemonic TEXT NOT NULL,\n" + "	name TEXT\n" + ");";

		String sqlTblAsc = "CREATE TABLE  GenAssociations (\n" + "	fromObjid            INTEGER,\n"
				+ "  ascType                 INTEGER NOT NULL,\n" + "  toObjid                  INTEGER NOT NULL,\n"
				+ "  inverseAscType   INTEGER NOT NULL,\n" + "  ascMnemonic      TEXT NOT NULL,\n"
				+ "  card                        TEXT NOT NULL,\n" + "	direction               TEXT,\n" 
				+ "  seqno                      INTEGER NOT NULL,\n"  
				+ "PRIMARY KEY (fromObjid, ascType, seqno)"
				+ ");";

		String sqlTblPrp = "CREATE TABLE  GenProperties (\n" + "	objid            INTEGER,\n"
				+ "  prpType                 INTEGER NOT NULL,\n"  
				+ "  mnemonic            TEXT NOT NULL,\n" + "  format                   TEXT NOT NULL,\n"
				+ "	value                      TEXT,\n"
				+ "PRIMARY KEY (objid, prpType)"
				+ ");";

		try {
			Statement stmt = connection.createStatement();

			stmt.execute(droptbl1);
			stmt.execute(droptbl2);
			stmt.execute(droptbl3);

			System.out.println("Tables dropped");

			stmt.execute(sqlTblObj);
			stmt.execute(sqlTblAsc);
			stmt.execute(sqlTblPrp);

			System.out.println("Tables created");
			
			connection.setAutoCommit(false);
	    	extractObjectsAndProperties();
			extractAssociations();
			connection.commit();
			stmt.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (EncyUnsupportedOperationException e) {
			System.out.println(e.getMessage());
		}
	}

	/*
	 * Populates  tables creating model objects and their properties.
	 */
	private void extractObjectsAndProperties() throws EncyUnsupportedOperationException, SQLException {
		System.out.println("Loading objects and properties...");
		String queryObj = "INSERT INTO GenObjects  (id, objType, objMnemonic, name ) VALUES (?,?,?,?);";
		PreparedStatement statementObj = connection.prepareStatement(queryObj);
		
		String queryPrp = "INSERT INTO GenProperties  (objid, prpType, mnemonic, format, value ) VALUES (?,?,?,?,?);";
		PreparedStatement statementPrp = connection.prepareStatement(queryPrp);

		for (ObjId objId : model.getObjIds()) {
			MMObj mmObj = MMObj.getInstance(model, objId);
			statementObj.setLong(1, objId.getValue());
			statementObj.setShort(2, ObjTypeHelper.getCode(mmObj.getObjTypeCode()));
			statementObj.setString(3, ObjTypeHelper.getMnemonic(mmObj.getObjTypeCode()));

			List<PrpTypeCode> listprp = ObjTypeHelper.getProperties(mmObj.getObjTypeCode());
			for (PrpTypeCode prp : listprp) {
				PrpFormat format = PrpTypeHelper.getFormat(mmObj.getObjTypeCode(), prp);
				
				statementPrp.setLong(1, objId.getValue());
				statementPrp.setShort(2,  PrpTypeHelper.getCode(prp));
				statementPrp.setString(3, prp.name());
				statementPrp.setString(4, format.name());
				
				if (format == PrpFormat.TEXT || format == PrpFormat.LOADNAME || format == PrpFormat.NAME) {
					String textValue = mmObj.getTextProperty(prp);
					if (textValue != PrpTypeHelper.getDefaultTxtValue(mmObj.getObjTypeCode(), prp)
							&& textValue.length() != 0) {
						statementObj.setString(4, textValue);
						statementPrp.setString(5, textValue);
						statementPrp.executeUpdate();
						
						propertycount++;
						continue;
					}
				} else if (format == PrpFormat.CHAR) {
					char charValue = mmObj.getCharProperty(prp);
					if (charValue != PrpTypeHelper.getDefaultChrValue(mmObj.getObjTypeCode(), prp)) {

						statementPrp.setString(5, String.valueOf(charValue));
						statementPrp.executeUpdate();
						propertycount++;
						continue;
					}
				} else if (format == PrpFormat.INT || format == PrpFormat.SINT) {
					int intValue = mmObj.getIntProperty(prp);
					if (intValue != PrpTypeHelper.getDefaultIntValue(mmObj.getObjTypeCode(), prp)) {
						
						statementPrp.setString(5, String.valueOf(intValue));
						statementPrp.executeUpdate();
						propertycount++;
						continue;
					}
				}
			}
			statementObj.executeUpdate();
			objectcount++;
		}
	}
	
	/*
	 * Populates  tables creating model associations.
	 */
	private void extractAssociations() throws EncyUnsupportedOperationException, SQLException {
		System.out.println("Loading associations...");
		String queryAsc = "INSERT INTO GenAssociations  (fromObjid, ascType, toObjid, inverseAscType, ascMnemonic, card, direction, seqno ) VALUES (?,?,?,?,?,?,?,?);";
		PreparedStatement statementAsc = connection.prepareStatement(queryAsc);
		List<ObjId> objects = model.getObjIds();
		for (ObjId objId : objects) {
			MMObj mmObj = MMObj.getInstance(model, objId);
			List<AscTypeCode> listasc = ObjTypeHelper.getAssociations(mmObj.getObjTypeCode());
			for (AscTypeCode asc : listasc) {
				String direction = "B";
				if (AscTypeHelper.isForward(mmObj.getObjTypeCode(), asc)) {
					direction = "F";
				}
				if (AscTypeHelper.isOneToMany(mmObj.getObjTypeCode(), asc)) {
					List<MMObj> list = mmObj.followAssociationMany(asc);
					int seqno = 0;
					for (MMObj o : list) {
						statementAsc.setLong(1, objId.getValue());
						statementAsc.setShort(2, AscTypeHelper.getCode(asc));
						statementAsc.setLong(3, o.getId().getValue());
						statementAsc.setShort(4,
								AscTypeHelper.getCode(AscTypeHelper.getInverse(mmObj.getObjTypeCode(), asc)));
						statementAsc.setString(5, asc.name());
						statementAsc.setString(6, "M");
						statementAsc.setString(7, direction);
						statementAsc.setLong(8, seqno);
						statementAsc.executeUpdate();
						associationcount++;
						seqno = seqno + 1;
					}
				} else {
					MMObj one = mmObj.followAssociationOne(asc);
					if (one != null) {
						statementAsc.setLong(1, objId.getValue());
						statementAsc.setShort(2, AscTypeHelper.getCode(asc));
						statementAsc.setLong(3, one.getId().getValue());
						statementAsc.setShort(4,
								AscTypeHelper.getCode(AscTypeHelper.getInverse(mmObj.getObjTypeCode(), asc)));
						statementAsc.setString(5, asc.name());
						statementAsc.setString(6, "1");
						statementAsc.setString(7, direction);
						statementAsc.setLong(8, 0);
						statementAsc.executeUpdate();
						associationcount++;
					}
				}
			}
		}
	}

}
