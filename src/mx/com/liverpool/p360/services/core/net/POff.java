package mx.com.liverpool.p360.services.core.net;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;

public class POff {

	public static void main(String[] args) {
		try(DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
			
			@Override
			public void logE(Exception e) {
			}
			
			@Override
			public void log(String message) {
			}
		} )){
			DataRequestor dr = new DataRequestor(dastub);
			System.out.println( dr.sendPowerOff() );
		}
	}
	
}
