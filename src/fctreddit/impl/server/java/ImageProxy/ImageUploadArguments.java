package fctreddit.impl.server.java.ImageProxy;

import java.util.Base64;

public record ImageUploadArguments(String image, 
		String type, 
		String title, 
		String description) {

	public ImageUploadArguments(byte[] image, String title, String description) {
		this(Base64.getEncoder().encodeToString(image), "base64", title, description);
	}
	
	public byte[] getImageData() {
		return Base64.getDecoder().decode(this.image);
	}
}