package fctreddit.impl.server.java.ImageProxy;

public record AddImagesToAlbumArguments(String[] ids, String[] deletehashes) {

	public AddImagesToAlbumArguments(String imageId) {
		this(new String[]{imageId} , null);
	}
	
}