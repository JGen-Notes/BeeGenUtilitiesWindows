package eu.jgen.bee.extractor.json;

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

public class CAGenExtractor {

	private Model model;
	private Ency ency;
	private int objectcount;
	private int propertycount;
	private int associationcount;

	public static void main(String[] args) {

		System.out.println("Bee Gen Model Extractor, Version 0.1.");
		CAGenExtractor extractor = new CAGenExtractor();
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
			System.out.println("Problem with creating output stream.");
			e.printStackTrace();
		}
	}

	private void usage() {
		System.out.println("USAGE:");
		System.out.println(
				"\tpathModel      -   Location of the directory containing CA Gen Local Model (directory ending with .ief)");
		System.out.println(
				"\tpathOutput      -  Location of the directory to store generated files objects.json and association.json");
		System.out.println("\n");
	}

	private void start(String modelPath, String outputPath)
			throws EncyException, ModelNotFoundException, FileNotFoundException {
		ency = EncyManager.connectLocalForReadOnly(modelPath);
		model = ModelManager.open(ency, ency.getModelIds().get(0));

		FileOutputStream outputStreamForObjects = new FileOutputStream(outputPath + "objects.json");
		extractObjectsAndProperties(outputStreamForObjects);
		System.out.println("\tStarting...");
		FileOutputStream outputStreamForAssociations = new FileOutputStream(outputPath + "associations.json");
		extractAssociations(outputStreamForAssociations);
		System.out.println("\tNumber of exported objects is " + objectcount);
		System.out.println("\tNumber of exported properties is " + propertycount);
		System.out.println("\tNumber of exported associations is " + associationcount);
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
