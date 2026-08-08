package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

public class AnotherXMLHandlerFastProcessPipeline {
	
	public static void main(String[] args) throws Exception {
		AnotherXMLHandlerFastProcessProductName.main(args);
		Thread t = new Thread(() -> {
			try {
				AnotherXMLHandlerFastProcessProductSKUs.main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} );
		t.start();
		Thread t2 = new Thread(()-> {
			try {
				AnotherXMLHandlerFastProcessProductsTexts.main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} );
		t2.start();
		Thread t3 = new Thread(()-> {
			try {
				AnotherXMLHandlerFastProcessVariants.main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} );
		t3.start();
		Thread t4 = new Thread(()-> {
			try {
				AnotherXMLHandlerFastProcessTempalte.main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} );
		t4.start();
		Thread t5 = new Thread(()-> {
			try {
				AnotherXMLHandlerFastProcessRelations.main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} );
		t5.start();
		try {
			t.join();
			t2.join();
			t3.join();
			t4.join();
			t5.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Done." );
	}
}
