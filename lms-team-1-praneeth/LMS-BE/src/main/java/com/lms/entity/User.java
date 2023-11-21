package com.lms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "build")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "username")
	@NotEmpty(message = "name cannot be empty")
	private String name;

	@NotEmpty(message = "Email cannot be empty")
	private String email;

	@NotEmpty(message = "password cannot be empty")
	@JsonProperty(access = Access.WRITE_ONLY)
	private String password;

	@NotEmpty(message = "role cannot be empty")
	private String roles;

	@Lob
	@Column(columnDefinition = "LONGBLOB")
	private byte[] img;

}
