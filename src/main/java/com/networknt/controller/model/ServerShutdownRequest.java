package com.networknt.controller.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * POJO class for POST request body of shutdown endpoint.
 * 
 *
 */
public class ServerShutdownRequest {

	private String protocol;
	private String address;
	private Integer port;

	public ServerShutdownRequest() {
	}

	@JsonProperty("protocol")
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@JsonProperty("address")
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@JsonProperty("port")
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ServerShutdownRequest ServerShutdownRequest = (ServerShutdownRequest) o;

		return Objects.equals(protocol, ServerShutdownRequest.protocol)
				&& Objects.equals(address, ServerShutdownRequest.address)
				&& Objects.equals(port, ServerShutdownRequest.port);
	}

	@Override
	public int hashCode() {
		return Objects.hash(protocol, address, port);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class ServerShutdownRequest {\n");
		sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");
		sb.append("    address: ").append(toIndentedString(address)).append("\n");
		sb.append("    port: ").append(toIndentedString(port)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
