package com.lms.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.DataFormatException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lms.config.JwtService;
import com.lms.dto.AllCourseUsersDto;
import com.lms.dto.UserCoursesDto;
import com.lms.dto.UserDto;
import com.lms.dto.UserResponseDto;
import com.lms.dto.UserVerifyDto;
import com.lms.dto.VideoDto;
import com.lms.entity.CourseLink;
import com.lms.entity.CourseModules;
import com.lms.entity.CourseUsers;
import com.lms.entity.Courses;
import com.lms.entity.CoursesViewDto;
import com.lms.entity.CoursesViewDto.CoursesViewDtoBuilder;
import com.lms.entity.User;
import com.lms.exception.details.CustomException;
import com.lms.exception.details.EmailNotFoundException;
import com.lms.service.UserService;
import com.lms.serviceImpl.EmailService;
import com.lms.serviceImpl.OtpService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

//@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "User Controller", description = "Operations related to users")
public class UserController {

	@Autowired
	private UserService lus;

	@Autowired
	private JwtService js;

	@Autowired
	private AuthenticationManager am;

	@Autowired
	private OtpService otps;

	@Autowired
	private EmailService es;

	@GetMapping("/getallapi")
	public void redirectToSwagger(HttpServletResponse response) throws IOException {
		response.sendRedirect("/swagger-ui/index.html#/");
	}

	/*
	 * 
	 * API used to test the connect for connection of back-end with front-end
	 * 
	 */

	@GetMapping("/connect")
	public String login() {
		return "Connected To Back-End";
	}

	/*
	 * 
	 * API used to generate the and send the JWT token on login and name email
	 * 
	 */

	@PostMapping("/login")
	public ResponseEntity<UserResponseDto> getJwtToken(@RequestBody @Valid UserDto jwt)
			throws IOException, DataFormatException {

		try {
			Authentication authenticate = am
					.authenticate(new UsernamePasswordAuthenticationToken(jwt.getEmail(), jwt.getPassword()));

			if (authenticate.isAuthenticated()) {

				String genJwtToken = js.genJwtToken(jwt.getEmail());
				Optional<User> output = lus.fingbyemail(jwt.getEmail());

				byte[] downloadImage = lus.downloadImage(jwt.getEmail());
				String encodeToString = "";
				String img = "";

				if (downloadImage != null) {
					encodeToString = Base64.getEncoder().encodeToString(downloadImage);
					img = "data:image/png;base64" + encodeToString;
				} else {
					img = output.get().getName().substring(0, 2).toUpperCase();
				}

				UserCoursesDto uc = null;
				try {
					uc = lus.getCourseUsers(output.get().getEmail());

				} catch (Exception e) {

				}

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);

				UserResponseDto ld2 = new UserResponseDto(output.get().getId(), output.get().getName(), jwt.getEmail(),
						genJwtToken, output.get().getRoles(), img, uc);

				return ResponseEntity.ok().headers(headers).body(ld2);
			} else {
				throw new EmailNotFoundException("Email Not Found");
			}
		} catch (BadCredentialsException ex) {
			throw new CustomException("Password Incorrect");
		}
	}

	/*
	 * 
	 * API used to upload the image files to the DB based on the email
	 * 
	 */

	@PostMapping("/upload")
	public ResponseEntity<String> uploadImage(
			@RequestPart("file") @Valid @Size(max = 50000, message = "Image size is greater than 5MB") MultipartFile mp,
			String email) throws Exception {

		String uploadImage = lus.saveImg(mp, email);

		if (uploadImage.equals(null)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed");
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(uploadImage);
		}
	}

	/*
	 * 
	 * API used to retrieve the image from the DB using the email as parameter
	 * 
	 */

	@GetMapping("/{email}")
	public ResponseEntity<String> downloadImage(@PathVariable("email") String email)
			throws IOException, DataFormatException {
		byte[] imageData = lus.downloadImage(email);
		String encodeToString = "";
		String img = "";

		if (imageData != null) {
			encodeToString = Base64.getEncoder().encodeToString(imageData);
			img = "data:image/png;base64 " + encodeToString;
		} else {
			img = email.substring(0, 2).toUpperCase();
		}
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(img);

	}

	/*
	 * 
	 * API used to update the details of user
	 * 
	 */

	@PutMapping("/update")
	public ResponseEntity<String> learnerUserUpdate(@RequestBody User lu) {

		lus.Luupdate(lu);

		return new ResponseEntity<String>("User details updated", HttpStatus.ACCEPTED);

	}

	/*
	 * 
	 * API used to get otp for generating the otp for verify the account
	 * 
	 */

	@PostMapping("/getotp")
	public ResponseEntity<String> getotp(@RequestParam String email) throws Exception {

		String generateOtp = otps.generateOtp();

		es.sendOtpEmail(email, generateOtp);

		UserVerifyDto userVerifyDto = new UserVerifyDto(email, generateOtp, LocalDateTime.now());
		lus.saveotp(userVerifyDto);

		return new ResponseEntity<String>("OTP SENT", HttpStatus.OK);
	}

	/*
	 * 
	 * API used to verify the otp for verify the user and reseting the password
	 * 
	 */

	@GetMapping("/verifyacc")
	public ResponseEntity<String> verifyAccount(@RequestParam("email") String email, @RequestParam("otp") String otp) {
		boolean verifyAccount = lus.verifyAccount(email, otp);

		if (verifyAccount) {
			return new ResponseEntity<String>("OTP Verified", HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Invalid OTP", HttpStatus.BAD_REQUEST);
		}
	}

	/*
	 * 
	 * API used to reset the password and save the new password
	 * 
	 */

	@PostMapping("/resetpassword")
	public ResponseEntity<String> saveNewPassword(@RequestParam("password") String password,
			@RequestParam("verifypassword") String verifypassword, @RequestParam("id") long id) {

		boolean resetPassword = lus.resetPassword(password, verifypassword, id);

		if (resetPassword) {
			return new ResponseEntity<String>("Reset Password Done", HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Unable To Reset Password", HttpStatus.BAD_REQUEST);
		}

	}

	@PostMapping("/addcourseuser")
	public ResponseEntity<?> addCourseUser(
			@ApiParam(value = "User details", defaultValue = "{\"username\":\"\",\"useremail\":\"\"}") @RequestParam String username,
			@RequestParam String useremail) {

		CourseUsers cu = CourseUsers.builder().username(username).useremail(useremail).build();

		boolean saveUserCourse = lus.saveCourseUser(cu);

		if (saveUserCourse) {
			return new ResponseEntity<String>("CourseUsers Saved", HttpStatus.CREATED);
		} else {
			return new ResponseEntity<String>("Unable To Save CourseUsers", HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/addcourse")
	public ResponseEntity<?> addCourse(@RequestParam String coursename, @RequestParam String coursetrainer) {

		Courses cc = Courses.builder().coursename(coursename).coursetrainer(coursetrainer).build();

		boolean saveUserCourse = lus.saveCourses(cc);

		if (saveUserCourse) {
			return new ResponseEntity<String>("Courses Saved", HttpStatus.CREATED);
		} else {
			return new ResponseEntity<String>("Unable To Save Courses", HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/accesscoursetouser")
	public ResponseEntity<?> accessCouresToUser(@RequestParam String email, @RequestParam String cname,
			@RequestParam String trainername) {
		boolean accessTocoures = lus.accessCouresToUser(email, cname, trainername);

		if (accessTocoures) {
			return new ResponseEntity<String>("Course Added To User", HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Course Unable To Add User", HttpStatus.BAD_REQUEST);
		}

	}

	@PostMapping("/addvideolink")
	public String addVideolink(@RequestBody @Valid VideoDto vd) {
		lus.addVideoLink(vd);

		return "saved";
	}

	@GetMapping("/getcourseusers")
	public ResponseEntity<UserCoursesDto> getCourseUsers(@RequestParam String cuname) {

		UserCoursesDto uc = lus.getCourseUsers(cuname);

		if (uc == null) {
			throw new CustomException("No User Found");
		} else {
			return new ResponseEntity<UserCoursesDto>(uc, HttpStatus.OK);
		}

	}

	@GetMapping("/getcourse")
	public ResponseEntity<List<AllCourseUsersDto>> getCourses(@RequestParam String cname, String fname) {

		List<AllCourseUsersDto> uc = lus.getCourses(cname, fname);

		if (uc == null) {
			throw new CustomException("No User Found");
		} else {
			return new ResponseEntity<List<AllCourseUsersDto>>(uc, HttpStatus.OK);
		}

	}

	@GetMapping("/getvideos")
	public ResponseEntity<List<CoursesViewDto>> getVideo(@RequestParam String name, @RequestParam String cname,
			@RequestParam String tname) {

		List<CourseModules> videoLink = lus.getVideoLink(name, cname, tname);

		List<Integer> mn = videoLink.stream().map(x -> x.getModulenum()).collect(Collectors.toList());

		List<List<CourseLink>> collect = videoLink.stream().map(x -> x.getClinks()).collect(Collectors.toList());

		List<List<CourseLink>> findFirst = collect.stream().toList();

		List<List<String>> listoflinks = findFirst.stream().flatMap(clinks -> clinks.stream().map(CourseLink::getLink))
				.collect(Collectors.toList());

		List<List<String>> listofvideonames = findFirst.stream()
				.flatMap(clinks -> clinks.stream().map(CourseLink::getVideoname)).collect(Collectors.toList());

//		Collections.reverse(listofvideonames);

		List<Map<String, String>> resultMapList = new ArrayList<>();

		for (int i = 0; i < listoflinks.size(); i++) {
			List<String> list2 = listoflinks.get(i);
			List<String> list3 = listofvideonames.get(i);

			Map<String, String> resultMap = new HashMap<>();

			for (int j = 0; j < list2.size(); j++) {

				resultMap.put(list3.get(j), list2.get(j));
			}

			resultMapList.add(resultMap);
		}

		List<CoursesViewDtoBuilder> combinedList = IntStream.range(0, Math.min(mn.size(), resultMapList.size()))
				.mapToObj(i -> CoursesViewDto.builder().modulenum(mn.get(i)).videos(resultMapList.get(i)))
				.collect(Collectors.toList());

		List<CoursesViewDto> list = combinedList.stream().map(CoursesViewDtoBuilder::build)
				.collect(Collectors.toList());

		return new ResponseEntity<List<CoursesViewDto>>(list, HttpStatus.OK);
	}

}