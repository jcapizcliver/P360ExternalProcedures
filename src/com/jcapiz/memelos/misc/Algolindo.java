package com.jcapiz.memelos.misc;

public class Algolindo {


	public static void main(String[] args) {
		try(
			java.net.Socket socket = new java.net.Socket(args[0], Integer.parseInt(args[1]));
			java.io.FileInputStream fis = new java.io.FileInputStream(args[2])
		){
			System.out.println("Now sending...");
			int length;
			byte[] chunk = new byte[1024];
			long cnt = 0;
			while((length = fis.read(chunk)) != -1) {
				socket.getOutputStream().write(chunk, 0, length);
				cnt += length;
				if(cnt % (1024*5) == 0) {
					System.out.println(cnt + " bytes. " + (cnt/1024) + " KB. " + (cnt/(1024^2)) + "MB. " + (cnt/(1024)^3) + "GB.");
				}
			}
			System.out.println("Done.");
		}catch(java.io.IOException e) {

		}
	}
}
