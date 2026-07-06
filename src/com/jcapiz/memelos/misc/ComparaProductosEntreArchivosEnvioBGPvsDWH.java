package com.jcapiz.memelos.misc;

public class ComparaProductosEntreArchivosEnvioBGPvsDWH {

	public static void main(String[] args) {
		java.util.Map<String, Integer> productosEnvioBGP = new java.util.TreeMap<>();
		java.util.Map<String, Integer> productosEnvioDWH = new java.util.TreeMap<>();
		Integer freq = null;
		String pid = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("<Product ID=\"([A-Za-z0-9]+)\"");
		java.util.regex.Matcher m = null;
		String line = null;
		System.out.println("\n\n\t******************* Now reading BGP files *******************");
		java.io.File[] fls = new java.io.File("D:\\tmp\\muestras\\BGP\\BGP_82327468").listFiles(ff->ff.getName().endsWith(".xml"));
		for(java.io.File f : fls) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				while((line = br.readLine()) != null) {
					m = p.matcher(line);
					if(m.find()) {
						pid = m.group(1);
						freq = productosEnvioBGP.get(pid);
						productosEnvioBGP.put(pid, (freq == null ? 0 : freq) + 1);
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("\n\n\t******************* Now reading DWH files *******************");
		fls = new java.io.File("D:\\tmp\\muestras\\BGP\\BGP_82326611").listFiles(ff->ff.getName().endsWith(".xml"));
		for(java.io.File f : fls) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				while((line = br.readLine()) != null) {
					m = p.matcher(line);
					if(m.find()) {
						pid = m.group(1);
						freq = productosEnvioBGP.get(pid);
						productosEnvioDWH.put(pid, (freq == null ? 0 : freq) + 1);
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		java.util.LinkedList<java.util.Map.Entry<String, Integer>> entrySetListBGP = new java.util.LinkedList<>(productosEnvioBGP.entrySet());
		java.util.LinkedList<java.util.Map.Entry<String, Integer>> entrySetListDWH = new java.util.LinkedList<>(productosEnvioDWH.entrySet());
		System.out.println("\n\n\t******************* Now sorting BGP entries (" + entrySetListBGP.size() + ") *******************");
		java.util.Collections.sort(entrySetListBGP, (o1,o2)->o2.getValue().compareTo(o1.getValue()));
		System.out.println("\n\n\t******************* Now sorting DWH entries (" + entrySetListDWH.size() + ") *******************");
		java.util.Collections.sort(entrySetListDWH, (o1,o2)->o2.getValue().compareTo(o1.getValue()));
//		System.out.println("\n\n\t******************* BGP *******************");
//		entrySetListBGP.forEach(System.out::println);
//		System.out.println("\n\n\t******************* DWH *******************");
//		entrySetListDWH.forEach(System.out::println);
		java.util.Set<String> mutualOnes = new java.util.TreeSet<>();
		java.util.LinkedList<java.util.Map.Entry<String, Integer>> shortest = entrySetListBGP.size() < entrySetListDWH.size() ? entrySetListBGP : entrySetListDWH;
		java.util.Map<String, Integer> reference = shortest.equals(entrySetListBGP) ? productosEnvioDWH : productosEnvioBGP;
		for(java.util.Map.Entry<String, Integer> entry : shortest) {
			if(reference.containsKey(entry.getKey())) {
				mutualOnes.add(entry.getKey());
			}
		}
		System.out.println("\n\n\t******************* MUTUAL *******************");
		mutualOnes.forEach(System.out::println);
		java.util.Set<String> anotherTry = new java.util.TreeSet<>();
		for(java.util.Map.Entry<String, Integer> entry : productosEnvioBGP.entrySet()) {
			if(productosEnvioDWH.containsKey(entry.getKey())) {
				anotherTry.add(entry.getKey());
			}
		}
		System.out.println("\n\n\t******************* MUTUAL(another) *******************");
		anotherTry.forEach(System.out::println);
	}

}
