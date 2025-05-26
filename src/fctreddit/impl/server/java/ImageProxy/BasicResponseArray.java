package fctreddit.impl.server.java.ImageProxy;

import java.util.List;

public class BasicResponseArray {

	private List<?> data;
	private int status;
	private boolean success;

	public List<?> getData() {
		return data;
	}

	public void setData(List<?> data) {
		this.data = data;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

}