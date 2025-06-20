package fctreddit.impl.server.java.ImageProxy;

public record CreateAlbumArguments(String title, 
		String description, 
		String privacy, 
		String layout, 
		String cover, 
		String[] ids, 
		String[] deletedhashes) {
	
	public CreateAlbumArguments(String title) {
		this(title, title, "public", "grid", null, null, null);
	}
	
}