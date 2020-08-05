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
package eu.jgen.bee.extractor.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
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

public class BeeGenExtractorJSON {
	
	private static final String ASSOCIATIONS_JSON = "associations.json";
	private static final String OBJECTS_JSON = "objects.json";
	private static final String STRING_SLASH = "\\";
	private String BEE_FOLDER_NAME = "bee"; 

	private Model model;
	private Ency ency;
	private int objectcount;
	private int propertycount;
	private int associationcount;
	private String modelName ="UNKNOWN";

	public static void main(String[] args) {

		System.out.println("Bee Gen  Model Extractor, Version 0.2");
		BeeGenExtractorJSON extractor = new BeeGenExtractorJSON();
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
			System.out.println("Problem with creating output stream.");
			e.printStackTrace();
		}
	}

	private void usage() {
		System.out.println("USAGE:");
		System.out.println(
				"\tpathModel      -   Location of the directory containing CA Gen Local Model (directory ending with .ief)");
		System.out.println("\n");
	}

	/*
	 * Two json files will be created in a new bee sub-folder of the <your-model> .ief folder.
	 * Previous files will be overwritten by a newly created ones.
	 */
	private void start(String modelPath)
			throws EncyException, ModelNotFoundException, FileNotFoundException {
		 System.out.println("Connecting to the CA Gen Model\n");
		ency = EncyManager.connectLocalForReadOnly(modelPath);
		model = ModelManager.open(ency, ency.getModelIds().get(0));
		modelName = model.getName();
		
		String outputPath = clearTargetDestination(modelPath);
		System.out.println("Connected to the model " + modelName + ".");
		
		FileOutputStream outputStreamForObjects = new FileOutputStream(outputPath +  STRING_SLASH +OBJECTS_JSON);
		extractObjectsAndProperties(outputStreamForObjects);
		System.out.println("\tStarting...");
		FileOutputStream outputStreamForAssociations = new FileOutputStream(outputPath +  STRING_SLASH + ASSOCIATIONS_JSON);
		extractAssociations(outputStreamForAssociations);
		System.out.println("\tNumber of exported objects is " + objectcount);
		System.out.println("\tNumber of exported properties is " + propertycount);
		System.out.println("\tNumber of exported associations is " + associationcount);
	}
	
	private String clearTargetDestination(String modelPath) {
		File file = new File(modelPath);
		if(! file.isDirectory()) {
			System.out.println("Specified model path is not a correct folder.");
			System.exit(9);
		}
		file = new File(modelPath + STRING_SLASH + BEE_FOLDER_NAME);
		if (file.exists()) {
			file = new File(modelPath + STRING_SLASH + BEE_FOLDER_NAME + STRING_SLASH +  OBJECTS_JSON);
			file.delete();
			file = new File(modelPath + STRING_SLASH + BEE_FOLDER_NAME + STRING_SLASH +  ASSOCIATIONS_JSON);
			file.delete();
			return modelPath + STRING_SLASH + BEE_FOLDER_NAME;
		}
		if (file.mkdir()) {
			return modelPath + STRING_SLASH + BEE_FOLDER_NAME;
		}
		return null;
	}

	/*
	 * Creates JSON file containing all model objects and their properties.
	 */
	private void extractObjectsAndProperties(FileOutputStream outputStream)
			throws EncyUnsupportedOperationException, FileNotFoundException {
		List<ObjId> objects = model.getObjIds();
		JsonArrayBuilder objectsArray = Json.createArrayBuilder();
		for (ObjId objId : objects) {
			MMObj mmObj = MMObj.getInstance(model, objId);
			JsonObjectBuilder valueObject = Json.createObjectBuilder().add("id", objId.getValue())
					.add("type", ObjTypeHelper.getCode(mmObj.getObjTypeCode()))
					.add("mnemonic", mmObj.getObjTypeCode().name());
			JsonArrayBuilder propertiesArray = Json.createArrayBuilder();
			List<PrpTypeCode> listprp = ObjTypeHelper.getProperties(mmObj.getObjTypeCode());
			for (PrpTypeCode prp : listprp) {
				PrpFormat format = PrpTypeHelper.getFormat(mmObj.getObjTypeCode(), prp);
				if (format == PrpFormat.TEXT || format == PrpFormat.LOADNAME || format == PrpFormat.NAME) {
					String textValue = mmObj.getTextProperty(prp);
					if (textValue != PrpTypeHelper.getDefaultTxtValue(mmObj.getObjTypeCode(), prp)
							&& textValue.length() != 0) {
						JsonObjectBuilder valueProperty = Json.createObjectBuilder()
								.add("type", PrpTypeHelper.getCode(prp)).add("format", format.name())
								.add("mnemonic", prp.name()).add("value", textValue);
						propertiesArray.add(valueProperty);
						propertycount++;
						continue;
					}
				} else if (format == PrpFormat.CHAR) {
					char charValue = mmObj.getCharProperty(prp);
					if (charValue != PrpTypeHelper.getDefaultChrValue(mmObj.getObjTypeCode(), prp)) {
						JsonObjectBuilder valueProperty = Json.createObjectBuilder()
								.add("type", PrpTypeHelper.getCode(prp)).add("format", format.name())
								.add("mnemonic", prp.name()).add("value", String.valueOf(charValue));
						propertiesArray.add(valueProperty);
						propertycount++;
						continue;
					}
				} else if (format == PrpFormat.INT || format == PrpFormat.SINT) {
					int intValue = mmObj.getIntProperty(prp);
					if (intValue != PrpTypeHelper.getDefaultIntValue(mmObj.getObjTypeCode(), prp)) {
						JsonObjectBuilder valueProperty = Json.createObjectBuilder()
								.add("type", PrpTypeHelper.getCode(prp)).add("format", format.name())
								.add("mnemonic", prp.name()).add("value", String.valueOf(intValue));
						propertiesArray.add(valueProperty);
						propertycount++;
						continue;
					}
				}
			}
			valueObject.add("properties", propertiesArray);
			objectsArray.add(valueObject);
			objectcount++;
		}
		JsonArray arr = objectsArray.build();
		JsonWriter writer = Json.createWriter(outputStream);
		writer.writeArray(arr);
		writer.close();
	}

	private void extractAssociations(FileOutputStream outputStream) throws EncyUnsupportedOperationException {

		List<ObjId> objects = model.getObjIds();
		JsonArrayBuilder assocArray = Json.createArrayBuilder();
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
						JsonObjectBuilder valueAssociation = Json.createObjectBuilder().add("from", objId.getValue())
								.add("card", "M").add("mnemonic", asc.name()).add("type", AscTypeHelper.getCode(asc))
								.add("inverseType",
										AscTypeHelper.getCode(AscTypeHelper.getInverse(mmObj.getObjTypeCode(), asc)))
								.add("to", o.getId().getValue()).add("seqno", seqno).add("direction", direction);
						;
						assocArray.add(valueAssociation.build());
						associationcount++;
						seqno = seqno + 1;
					}
				} else {
					MMObj one = mmObj.followAssociationOne(asc);
					if (one != null) {
						JsonObjectBuilder valueAssociation = Json.createObjectBuilder().add("from", objId.getValue())
								.add("card", "1").add("mnemonic", asc.name()).add("type", AscTypeHelper.getCode(asc))
								.add("inverseType",
										AscTypeHelper.getCode(AscTypeHelper.getInverse(mmObj.getObjTypeCode(), asc)))
								.add("to", one.getId().getValue()).add("seqno", 0).add("direction", direction);
						assocArray.add(valueAssociation.build());
						associationcount++;
					}
				}
			}
		}
		JsonArray arr = assocArray.build();
		JsonWriter writer = Json.createWriter(outputStream);
		writer.writeArray(arr);
		writer.close();
	}
}
