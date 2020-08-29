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

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;

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
import com.ca.gen.jmmi.schema.ObjTypeCode;
import com.ca.gen.jmmi.schema.ObjTypeHelper;
import com.ca.gen.jmmi.schema.PrpTypeCode;
import com.ca.gen.jmmi.schema.PrpTypeHelper;
import com.ca.gen.jmmi.util.PrpFormat;

/*
 * This class allows extract design metadata from the CA Gen Local Model and load
 * data to the purpose build SQLite database. The SQLite database constitutes 
 * Bee Gen Model that has its own API supporting essential basic inquiries, which
 * is a simililar to what CA Gen API offers. The Bee Gen API does not support 
 * adding new objects and updating existing ones.
 * 
 */
public class BeeGenExtractorSQLite {
	
	private static String VERSION = "0.4";
	private String SCHEMA = "9.2.A6";

	private static final String STRING_SLASH = "\\";
	private String BEE_FOLDER_NAME = "bee";

	private Connection connection = null;
	private Model model;
	private Ency ency;
	private int objectcount;
	private int propertycount;
	private int associationcount;
	private int objectmetacount;
	private int propertymetacount;
	private int associationmetacount;
	
	private String modelName = "UNKNOWN";

	public static void main(String[] args) {

		System.out.println("Bee Gen Model Creator, Version " + VERSION);
		BeeGenExtractorSQLite extractor = new BeeGenExtractorSQLite();
		try {
			extractor.usage();
			extractor.start(args[0]);
			System.out.println("Extraction completed.");
		} catch (EncyException e) {
			System.out.println("Problem with connecting to the encyclopedia.");
			e.printStackTrace();
		} catch (ModelNotFoundException e) {
			System.out.println("Cannot find model in the encyclopedia.");
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
		System.out.println("\n");
	}

	private void start(String modelPath) throws EncyException, ModelNotFoundException, FileNotFoundException {
		System.out.println("Connecting to the CA Gen Model\n");
		ency = EncyManager.connectLocalForReadOnly(modelPath);
		model = ModelManager.open(ency, ency.getModelIds().get(0));
		modelName = model.getName();
		String outputPath = clearTargetDestination(modelPath);
		System.out.println("Connected to the model " + modelName + ".");
		createDatabase(outputPath);

		System.out.println("\nStatistics:\n");
		System.out.println("\tNumber of exported objects is " + objectcount);
		System.out.println("\tNumber of exported properties is " + propertycount);
		System.out.println("\tNumber of exported associations is " + associationcount);
		
		System.out.println("\tNumber of exported meta objects is " + objectmetacount);
		System.out.println("\tNumber of exported meta properties is " + propertymetacount);
		System.out.println("\tNumber of exported meta associations is " + associationmetacount); 
		System.out.println("\n");
	}

	/*
	 * The Bee Gen Model will be created in a new bee sub-folder of the <your-model>
	 * .ief folder. Previous model will be overwritten by a newly created one.
	 */
	private String clearTargetDestination(String modelPath) {
		File file = new File(modelPath);
		if (!file.isDirectory()) {
			System.out.println("Specified model path is not a correct folder.");
			System.exit(9);
		}
		file = new File(modelPath + STRING_SLASH + BEE_FOLDER_NAME);
		if (file.exists()) {
			file = new File(modelPath + STRING_SLASH + BEE_FOLDER_NAME + STRING_SLASH + modelName + ".db");
			file.delete();
			return modelPath + STRING_SLASH + BEE_FOLDER_NAME;
		}
		if (file.mkdir()) {
			return modelPath + STRING_SLASH + BEE_FOLDER_NAME;
		}
		return null;
	}

	private void createDatabase(String outputPath) {

		try {
			Class.forName("org.sqlite.JDBC");
			final SQLiteConfig config = new SQLiteConfig();
			config.setJournalMode(JournalMode.OFF);
			connection = config.createConnection("jdbc:sqlite:" + outputPath + STRING_SLASH + modelName + ".db");

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Opened database successfully");
		
		String droptbl1 = "DROP TABLE IF EXISTS  GenObjects;";
		String droptbl2 = "DROP TABLE  IF EXISTS GenAssociations;";
		String droptbl3 = "DROP TABLE  IF EXISTS GenProperties;";

		String droptbl4 = "DROP TABLE  IF EXISTS GenMetaObjects;";
		String droptbl5 = "DROP TABLE  IF EXISTS GenMetaAssociations;";
		String droptbl6 = "DROP TABLE  IF EXISTS GenMetaProperties;";
		
		String droptbl7 = "DROP TABLE IF EXISTS  GenModel;";


		String sqlTblObj = "CREATE TABLE  GenObjects (\n" + "	id INTEGER PRIMARY KEY,\n"
				+ "  objType  INTEGER NOT NULL,\n" + "  objMnemonic TEXT NOT NULL,\n" + "	name TEXT\n" + ");";

		String sqlTblAsc = "CREATE TABLE  GenAssociations (\n" + "	fromObjid            INTEGER,\n"
				+ "  ascType                 INTEGER NOT NULL,\n" + "  toObjid                  INTEGER NOT NULL,\n"
				+ "  inverseAscType   INTEGER NOT NULL,\n" + "  ascMnemonic      TEXT NOT NULL,\n"
				+ "  card                        TEXT NOT NULL,\n" + "	direction               TEXT,\n"
				+ "  seqno                      INTEGER NOT NULL,\n" + "PRIMARY KEY (fromObjid, ascType, seqno)" + ");";

		String sqlTblPrp = "CREATE TABLE  GenProperties (\n" + "	objid            INTEGER,\n"
				+ "  prpType                 INTEGER NOT NULL,\n" + "  mnemonic            TEXT NOT NULL,\n"
				+ "  format                   TEXT NOT NULL,\n" + "	value                      TEXT,\n"
				+ "PRIMARY KEY (objid, prpType)" + ");";

		String sqlTblMetaObj = "CREATE TABLE  GenMetaObjects (\n" + "	objType INTEGER PRIMARY KEY,\n"
				+ " objMnemonic TEXT NOT NULL);";		

		String sqlTblMetaPrp = "CREATE TABLE  GenMetaProperties (\n" 
				+ "	objType            INTEGER NOT NULL,\n"
				+ " prpType            INTEGER NOT NULL,\n" 
				+ " prpMnemonic        TEXT NOT NULL,\n"
				+ " format             TEXT NOT NULL,\n" 
				+ " length             INTEGER NOT NULL,\n"
	 			+ "	defaultInt         INTEGER NOT NULL,\n" 
	 			+ "	defaultText        TEXT NOT NULL,\n"
	 			+ "	defaultChar        TEXT NOT NULL,\n"
				+ "PRIMARY KEY (objtype, prpType)"
				+ ");";
		
		String sqlTblMetaAsc = "CREATE TABLE  GenMetaAssociations (\n" 
				+ "	fromObjType         INTEGER NOT NULL,\n"
				+ " ascType             INTEGER NOT NULL,\n" 
				+ " ascMnemonic         TEXT NOT NULL,\n"
				+ "	direction           TEXT NOT NULL,\n" 
				+ " inverseAscType      INTEGER NOT NULL,\n"
				+ " optionality         TEXT NOT NULL,\n" 
				+ " card                TEXT NOT NULL,\n"
				+ " ordered             TEXT NOT NULL,\n" 
				+ "PRIMARY KEY (fromObjType, ascType)" 
				+ ");";

		String sqlTblModel = "CREATE TABLE GenModel (\n"
				+ " key TEXT NOT NULL PRIMARY KEY,\n"
				+ " value TEXT NOT NULL"
				+ ");";
		
		
		try {
			Statement stmt = connection.createStatement();

			stmt.execute(droptbl1);
			stmt.execute(droptbl2);
			stmt.execute(droptbl3);
			stmt.execute(droptbl4);
			stmt.execute(droptbl5);
			stmt.execute(droptbl6);
			stmt.execute(droptbl7);

			System.out.println("Tables dropped");

			stmt.execute(sqlTblObj);
			stmt.execute(sqlTblAsc);
			stmt.execute(sqlTblPrp);
			
			stmt.execute(sqlTblMetaObj);			
			stmt.execute(sqlTblMetaPrp);			
			stmt.execute(sqlTblMetaAsc);
			
			stmt.execute(sqlTblModel);

			System.out.println("Tables created");

			connection.setAutoCommit(false);
			
			extractObjectsAndProperties();
			
			populateModel();
			
//			extractAssociations();
//			
//			extractMeatDataForObjects();
//			extractMeatDataForProperties();
//			extractMeatDataForAssociations();
			
//			generateEnumForObjects();
//			generateEnumForProperties();
			generateEnumForAssociations();
			
			connection.commit();
			stmt.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (EncyUnsupportedOperationException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void populateModel() throws SQLException {
		String queryModel = "INSERT INTO GenModel  (key, value) VALUES (?,?);";
		PreparedStatement statementModel = connection.prepareStatement(queryModel);
		statementModel.setString(1, "name");
		statementModel.setString(2, model.getName());
		statementModel.executeUpdate();
		statementModel.setString(1, "version");
		statementModel.setString(2, VERSION);
		statementModel.executeUpdate();
		statementModel.setString(1, "schema");
		statementModel.setString(2, SCHEMA);
		statementModel.executeUpdate();
		statementModel.close();
	}
	
	private void generateEnumForProperties() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("package eu.jgen.beegen.model.meta;\n");
		buffer.append("public enum PrpMetaType {\n");
		for (PrpTypeCode prpTypeCode : PrpTypeCode.values()) {
			short code = PrpTypeHelper.getCode(prpTypeCode);
			if (code != -1) {
				buffer.append("\t" + PrpTypeHelper.getMnemonic(prpTypeCode) + "((short) " + code + "),\n");
			}
		}
		buffer.append("\tINVALID((short) -1)\n");
		buffer.append("\tDISCOVER((short) -2)\n");
		buffer.append("\n\tprivate final short code;\n");
		buffer.append("\n\tPrpMetaType(short code) {\n");
		buffer.append("\t\this.code = code;\n");
		buffer.append("\t}\n");
		buffer.append("\n\tpublic PrpMetaType getType(short code) {\n");
		buffer.append("\t\tfor (PrpMetaType obj : PrpMetaType.values()) {\n");
		buffer.append("\t\t\tif (obj.code == code) {\n");
		buffer.append("\t\t\t\treturn obj;\n");
		buffer.append("\t\t\t}\n");
		buffer.append("\t\t}\n");
		buffer.append("\t\treturn PrpMetaType.INVALID;\n");
		buffer.append("\t}\n");		
		buffer.append("}");
		System.out.println(buffer);		
	}
	
	private void generateEnumForAssociations() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("package eu.jgen.beegen.model.meta;\n");
		buffer.append("public enum AscMetaType {\n");
		for (AscTypeCode ascTypeCode : AscTypeCode.values()) {
			short code = AscTypeHelper.getCode(ascTypeCode);
			if (code != -1) {
				buffer.append("\t" + AscTypeHelper.getMnemonic(ascTypeCode) + "((short) " + code + "),\n");
			}
		}
		buffer.append("\tINVALID((short) -1)\n");
		buffer.append("\tDISCOVER((short) -2)\n");
		buffer.append("\n\tprivate final short code;\n");
		buffer.append("\n\tAscMetaType(short code) {\n");
		buffer.append("\t\this.code = code;\n");
		buffer.append("\t}\n");
		buffer.append("\n\tpublic AscMetaType getType(short code) {\n");
		buffer.append("\t\tfor (AscMetaType obj : AscMetaType.values()) {\n");
		buffer.append("\t\t\tif (obj.code == code) {\n");
		buffer.append("\t\t\t\treturn obj;\n");
		buffer.append("\t\t\t}\n");
		buffer.append("\t\t}\n");
		buffer.append("\t\treturn AscMetaType.INVALID;\n");
		buffer.append("\t}\n");		
		buffer.append("}");
		System.out.println(buffer);		
	}

	
	private void generateEnumForObjects() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("package eu.jgen.beegen.model.meta;\n");
		buffer.append("public enum ObjMetaType {\n");
		for (ObjTypeCode objTypeCode : ObjTypeCode.values()) {
			short code = ObjTypeHelper.getCode(objTypeCode);
			if (code != -1) {
				buffer.append("\t" + ObjTypeHelper.getMnemonic(objTypeCode) + "((short) " + code + "),\n");
			}
		}
		buffer.append("\tINVALID((short) -1)\n");
		buffer.append("\tDISCOVER((short) -2)\n");
		buffer.append("\n\tprivate final short code;\n");
		buffer.append("\n\tObjMetaType(short code) {\n");
		buffer.append("\t\this.code = code;\n");
		buffer.append("\t}\n");
		buffer.append("\n\tpublic ObjMetaType getType(short code) {\n");
		buffer.append("\t\tfor (ObjMetaType obj : ObjMetaType.values()) {\n");
		buffer.append("\t\t\tif (obj.code == code) {\n");
		buffer.append("\t\t\t\treturn obj;\n");
		buffer.append("\t\t\t}\n");
		buffer.append("\t\t}\n");
		buffer.append("\t\treturn ObjMetaType.INVALID;\n");
		buffer.append("\t}\n");		
		buffer.append("}");
		System.out.println(buffer);		
	}

	
	
	/*
	 * Populates tables with meta data.
	 */
	
	private void extractMeatDataForAssociations() throws SQLException {
		System.out.println("Loading meta data for associations...");

		String queryMetaAsc = "INSERT INTO GenMetaAssociations  (fromObjType, ascType, ascMnemonic, direction, inverseAscType, optionality, card, ordered) VALUES (?,?,?,?,?,?,?,?);";
		PreparedStatement statementAsc = connection.prepareStatement(queryMetaAsc);
		for (ObjTypeCode objTypeCode : ObjTypeCode.values()) {
			if (objTypeCode == ObjTypeCode.HORIZUS ||
					objTypeCode == ObjTypeCode.GUIPROP) {
				continue;
			}
			//System.out.println(objTypeCode);
			for (AscTypeCode ascTypeCode : ObjTypeHelper.getAssociations(objTypeCode)) {
				//System.out.println("\t " + ascTypeCode);
				statementAsc.setInt(1, ObjTypeHelper.getCode(objTypeCode));
				statementAsc.setInt(2, AscTypeHelper.getCode(ascTypeCode));
				statementAsc.setString(3, AscTypeHelper.getMnemonic(ascTypeCode));
				if (AscTypeHelper.isForward(objTypeCode, ascTypeCode)) {
					statementAsc.setString(4, "F");
				} else {
					statementAsc.setString(4, "B");
				}			
				//statementAsc.setInt(5, AscTypeHelper.getCode(AscTypeHelper.getInverse(objTypeCode, ascTypeCode)));
				statementAsc.setInt(5,100);
				if (AscTypeHelper.isIgnorable(objTypeCode, ascTypeCode)) {
					statementAsc.setString(6, "Y");
				} else {
					statementAsc.setString(6, "N");
				}
				if (AscTypeHelper.isOneToMany(objTypeCode, ascTypeCode)) {
					statementAsc.setString(7, "M");
				} else {
					statementAsc.setString(7, "1");
				}
				if (AscTypeHelper.isOrdered(objTypeCode, ascTypeCode)) {
					statementAsc.setString(8, "Y");
				} else {
					statementAsc.setString(8, "N");
				}	
				statementAsc.executeUpdate();
				associationmetacount++;				
			}			
		}
	}

	private void extractMeatDataForProperties() throws SQLException {
		System.out.println("Loading meta data for properties...");
		String queryMetaPrp = "INSERT INTO GenMetaProperties  (objType, prpType, prpMnemonic, format, length, defaultInt, defaultText, defaultChar) VALUES (?,?,?,?,?,?,?,?);";
		PreparedStatement statementPrp = connection.prepareStatement(queryMetaPrp);
		for (ObjTypeCode objTypeCode : ObjTypeCode.values()) {
			for (PrpTypeCode prpTypeCode : ObjTypeHelper.getProperties(objTypeCode)) {
				statementPrp.setInt(1, ObjTypeHelper.getCode(objTypeCode));
				statementPrp.setInt(2, PrpTypeHelper.getCode(prpTypeCode));
				statementPrp.setString(3, PrpTypeHelper.getMnemonic(prpTypeCode));
				String format = PrpTypeHelper.getFormat(objTypeCode, prpTypeCode).name();
				statementPrp.setString(4,  format);
				int length = PrpTypeHelper.getLength(objTypeCode, prpTypeCode);
				statementPrp.setInt(5, length);
				
				switch (format) {
				case "NAME":
				case "LOADNAME":
				case "TEXT":
					statementPrp.setDouble(6, 0);
					if (length == 0) {
						statementPrp.setString(7,"");
					} else {
						statementPrp.setString(7, PrpTypeHelper.getDefaultTxtValue(objTypeCode, prpTypeCode));							
					}
					statementPrp.setString(8,"");
					break;
				case "INT":
				case "SINT":
					statementPrp.setDouble(6, PrpTypeHelper.getDefaultIntValue(objTypeCode, prpTypeCode));
					statementPrp.setString(7, "");	
					statementPrp.setString(8,"");
					break;					
				case "CHAR":
					statementPrp.setDouble(6, 0);	
					statementPrp.setString(7,"");
				 	statementPrp.setString(8, String.valueOf(PrpTypeHelper.getDefaultChrValue(objTypeCode, prpTypeCode)));
			    	break;
				
				default:
					statementPrp.setDouble(6, 0);
					statementPrp.setString(7,"");
					statementPrp.setString(8,"");
					break;
				}	    	 	
 				statementPrp.executeUpdate();
				propertymetacount++;
			}
		}
	}

	private void extractMeatDataForObjects() throws SQLException {
		System.out.println("Loading meta data for objects...");
		String queryMetaObj = "INSERT INTO GenMetaObjects  (objType, objMnemonic) VALUES (?,?);";
		PreparedStatement statementObj = connection.prepareStatement(queryMetaObj);
		for (ObjTypeCode objTypeCode : ObjTypeCode.values()) {
			statementObj.setInt(1, ObjTypeHelper.getCode(objTypeCode));
			statementObj.setString(2, objTypeCode.toString());
			statementObj.executeUpdate();
			objectmetacount++;
		}
	}

	/*
	 * Populates tables creating model objects and their properties.
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
				statementPrp.setShort(2, PrpTypeHelper.getCode(prp));
				statementPrp.setString(3, prp.name());
				statementPrp.setString(4, format.name());

				if (format == PrpFormat.TEXT || format == PrpFormat.LOADNAME || format == PrpFormat.NAME) {
					String textValue = mmObj.getTextProperty(prp);
					if (textValue != PrpTypeHelper.getDefaultTxtValue(mmObj.getObjTypeCode(), prp)
							&& textValue.length() != 0) {
						if (format == PrpFormat.NAME) {
							statementObj.setString(4, textValue);
						}
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
	 * Populates tables creating model associations.
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
