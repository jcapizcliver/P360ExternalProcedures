package mx.com.liverpool.p360.services.core;

public class DropIT {

	public static void main(String[] args) {
		java.math.BigDecimal costoBrutoSinIVA = new java.math.BigDecimal( "3000" );
		java.math.BigDecimal costoNetoSinIVA = new java.math.BigDecimal( "0" );
		java.math.BigDecimal descuento1 = new java.math.BigDecimal( "10" );
		java.math.BigDecimal descuento2 = new java.math.BigDecimal( "10" );

		java.math.BigDecimal escala1 = descuento1.divide(java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.TEN));
		java.math.BigDecimal escala2 = descuento2.divide(java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.TEN));
		java.math.BigDecimal a = costoBrutoSinIVA.multiply(java.math.BigDecimal.ONE.subtract(escala1));
		java.math.BigDecimal b = a.multiply(escala2);
		java.math.BigDecimal c = a.subtract(b);
		System.out.println(escala1);
		System.out.println(escala2);
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		costoNetoSinIVA = c; //( costoBrutoSinIVA.subtract (costoBrutoSinIVA.multiply( (descuento1.divide(new java.math.BigDecimal( 100 )) ))).multiply(descuento2.divide(new java.math.BigDecimal(100))).subtract  ((costoBrutoSinIVA.subtract (costoBrutoSinIVA.multiply(descuento1.divide(new java.math.BigDecimal(100)))))));
		costoNetoSinIVA.setScale(2, java.math.RoundingMode.HALF_UP);
		if(costoBrutoSinIVA != null) {
			if( costoNetoSinIVA != null && costoNetoSinIVA.floatValue() > 0f ) {
				System.out.println("CostoNetoSinIVA: " + costoNetoSinIVA.toPlainString());
			}else {
				System.out.println("El costo neto sin iva, ya que no pasó la validación es: " + costoNetoSinIVA);
			}
		}
	}
}
