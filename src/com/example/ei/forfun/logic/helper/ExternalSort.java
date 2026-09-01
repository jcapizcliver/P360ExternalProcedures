package com.example.ei.forfun.logic.helper;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ExternalSort {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.List<String> skuList = new java.util.ArrayList<>();
		SimpleDelimitedFileParser sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				skuList.add(row[0]);
			}
		} );
		sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Listado de Genericos.csv"));
		System.out.println("Read " + skuList.size() + " elements to reffer to.");
		java.util.Map<String, String[]> datas = new java.util.HashMap<>();
		java.util.List<String> idsParticipantes = new java.util.ArrayList<>();
		String[] skus = skuList.toArray( new String[] {} );
		java.util.Arrays.sort(skus);
		sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				if( java.util.Arrays.binarySearch(skus, row[1]) > -1 ) {
					datas.put(row[0], row);
					idsParticipantes.add(row[0]);
				}
			}
		} );
		sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260419_181653_Product2G_Data.csv"));
		System.out.println("Read " + datas.size() + " idsParticipantes.");
		String[] ids = idsParticipantes.toArray( new String[] {} );
		java.util.Arrays.sort(ids);
		java.util.Set<String> internalIdsParticipantes = new java.util.TreeSet<>();
		java.util.Map<String, String> artToProd = new java.util.HashMap<>();
		sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				if( java.util.Arrays.binarySearch(ids, row[0]) > -1 ) {
					internalIdsParticipantes.add(row[1]);
					artToProd.put(row[1], row[0]);
				}
			}
		} );
		sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260419_181720_Prod2GExt_to_ArtInternal.csv"));
		System.out.println("Read " + artToProd.size() + " artículos a productos.");
		String[] internalIds = internalIdsParticipantes.toArray( new String[] {} );
		java.util.Arrays.sort(internalIds);
		int[] lines = new int[] {0};
		java.util.Set<String> iFoundYou = new java.util.TreeSet<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "salidaFiltradaPorSKUDeProductosConImagenesDeDetalle.csv").toFile()) ))){
			pw.println( rw.getRw().serializeChunk( "Identifier,SKU,CurrentStatusLabel,FotoTomadaLiverpool,FotoTomadaLiverpool_Name,EnrichmentRejectionMessage,EnriquecidoEnForo,IDVariante,SKUVariante,ProductImageURL,ImagenesDeDetalle".split(",") ) );
			sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				if(row.length > 0) {
					if( java.util.Arrays.binarySearch(internalIds, row[0]) > -1 ) {
						String pid = artToProd.get(row[0]);
						if(pid != null) {
							String[] ladata = datas.get(pid);
							if(ladata != null) {
								iFoundYou.add(pid);
								String[] lanuevaData = new String[11];
								for(int i=0; i<7; i++) {
									lanuevaData[i] = i == 6 && ladata.length < 7 ? "" : ladata[i];
								}
								for(int i=7; i<11; i++) {
									lanuevaData[i] = i == 10 && row.length < 5 ? "" : row[i - 6];
								}
								pw.println( rw.getRw().serializeChunk( lanuevaData ) );
								lines[0]++;
							}
						}
					}
				}
			} );
			sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260419_184750_AgregadosDeImagenesDeDetalle.csv"));
			idsParticipantes.forEach(p -> {
				if(!iFoundYou.contains(p)) {
					String[] ladata = datas.get(p);
					if(ladata != null) {
						iFoundYou.add(p);
						String[] lanuevaData = new String[11];
						for(int i=0; i<7; i++) {
							lanuevaData[i] = i == 6 && ladata.length < 7 ? "" : ladata[i];
						}
						for(int i=7; i<11; i++) {
							lanuevaData[i] = "";
						}
						pw.println( rw.getRw().serializeChunk( lanuevaData ) );
						lines[0]++;
					}
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "notFound.csv").toFile()) ))){
			idsParticipantes.forEach(p -> {
				if(!iFoundYou.contains(p)) {
					pw.println( p );
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Had: " + lines[0] + " hits.");
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
}
