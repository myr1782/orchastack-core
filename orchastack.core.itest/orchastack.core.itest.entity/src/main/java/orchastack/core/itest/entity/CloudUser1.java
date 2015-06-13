package orchastack.core.itest.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.apache.openjpa.persistence.ExternalValues;

@Entity
public class CloudUser1  implements Serializable {

	public CloudUser1() {
		// TODO Auto-generated constructor stub
	}

	@Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private String userId;

	@Basic
	private String name;

	@Basic
	private String firtName;

	@Basic
	private String lastName;

	@Basic
	private String title;

	@Basic
	private String email;

	@Basic
	private String phoneNum;

	@Basic
	@ExternalValues({ "true=T", "false=F" })
	@org.apache.openjpa.persistence.Type(String.class)
	private boolean deleted = false;

	@Version
	private int version;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirtName() {
		return firtName;
	}

	public void setFirtName(String firtName) {
		this.firtName = firtName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNum() {
		return phoneNum;
	}

	public void setPhoneNum(String phoneNum) {
		this.phoneNum = phoneNum;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getUserId() {
		return userId;
	}

	public CloudUser1(String userId, String name) {
		super();
		if (userId == null)
			userId = UUID.randomUUID().toString();
		this.userId = userId;
		this.name = name;
	}

	public CloudUser1(String userId, String name, String email) {
		this(userId, name);
		this.email = email;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CloudUser1 other = (CloudUser1) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CloudUser [userId=" + userId + ", name=" + name + ", firtName="
				+ firtName + ", lastName=" + lastName + ", title=" + title
				+ ", email=" + email + ", phoneNum=" + phoneNum + ", deleted="
				+ deleted + "]";
	}

}
