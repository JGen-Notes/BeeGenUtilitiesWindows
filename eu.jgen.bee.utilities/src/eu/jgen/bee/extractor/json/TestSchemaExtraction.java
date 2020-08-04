package eu.jgen.bee.extractor.json;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.ca.gen.jmmi.schema.AscTypeCode;
import com.ca.gen.jmmi.schema.AscTypeHelper;
import com.ca.gen.jmmi.schema.ObjTypeCode;
import com.ca.gen.jmmi.schema.ObjTypeHelper;
import com.ca.gen.jmmi.schema.PrpTypeCode;
import com.ca.gen.jmmi.schema.PrpTypeHelper;

class TestSchemaExtraction {

	@Test
	void test() {
//		for  (ObjTypeCode c : ObjTypeCode.values()) {
//			
//			 
//			System.out.println("\tcase " + c.name() + " = " +  ObjTypeHelper.getCode(c));
//			 
////			for (PrpTypeCode p : ObjTypeHelper.getProperties(c)) {
////				System.out.println("\t" + p.name() + " (" + PrpTypeHelper.getCode(p) +")");
////			}
//			 
//		}
		
//		for (PrpTypeCode p : PrpTypeCode.values()) {
//			
//			System.out.println("\tcase " + p.name() + " = " + PrpTypeHelper.getCode(p));
//			
//		}
		
		for (AscTypeCode a : AscTypeCode.values()) {
			
			System.out.println("\tcase " + a.name() + " = " + AscTypeHelper.getCode(a));
			
		}
		
	}

}
