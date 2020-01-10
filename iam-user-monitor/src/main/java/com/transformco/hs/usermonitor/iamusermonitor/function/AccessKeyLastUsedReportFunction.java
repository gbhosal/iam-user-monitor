package com.transformco.hs.usermonitor.iamusermonitor.function;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.GetAccessKeyLastUsedRequest;
import com.amazonaws.services.identitymanagement.model.GetAccessKeyLastUsedResult;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.transformco.hs.usermonitor.iamusermonitor.vo.User;


@Component("AccessKeyLastUsedReportFunction")
public class AccessKeyLastUsedReportFunction implements Function<ScheduledEvent, String> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccessKeyLastUsedReportFunction.class);
	private final AmazonIdentityManagement iamClient = AmazonIdentityManagementClientBuilder.standard().build();
	private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	@Autowired
	private Environment environment;
	
	@Override
	public String apply(ScheduledEvent scheduledEvent) {
		final List<User> userList = new ArrayList<>();
		
		ListUsersRequest listUsersRequest = new ListUsersRequest().withMaxItems(200);
		boolean isTruncatedResult = true;
		while (isTruncatedResult) {
			ListUsersResult listUsersResult = iamClient.listUsers(listUsersRequest);			
			listUsersResult.getUsers().forEach(user -> {
				User userVO = new User();
				userVO.setUserId(user.getUserId());
				userVO.setUserName(user.getUserName());
				processAllAccessKeys(user.getUserName(), userVO);
				userList.add(userVO);
			});
			isTruncatedResult = listUsersResult.getIsTruncated();
			listUsersRequest.withMarker(listUsersResult.getMarker());
		}
		
		userList.forEach(user -> LOGGER.info("User = {}", user));
		writeCSVReport(userList);
		return "SUCCESS";
	}
	
	private void processAllAccessKeys(String userName, User user) {
		ListAccessKeysRequest listAccessKeysRequest = new ListAccessKeysRequest().withUserName(userName)
				.withMaxItems(100);
		boolean isTruncatedResult = true;
		while (isTruncatedResult) {
			ListAccessKeysResult listAccessKeysResult = iamClient.listAccessKeys(listAccessKeysRequest);
			if (listAccessKeysResult == null) {
				return;
			}
			isTruncatedResult = listAccessKeysResult.getIsTruncated();
			if (!CollectionUtils.isEmpty(listAccessKeysResult.getAccessKeyMetadata())) {
				listAccessKeysResult.getAccessKeyMetadata().stream()
						.filter(accessKeyMetadata -> "Active".equals(accessKeyMetadata.getStatus()))
						.forEach(accessKeyMetadata -> {
							lastAccessInfo(accessKeyMetadata.getAccessKeyId(), user);
						});
			}
			listAccessKeysRequest.withMarker(listAccessKeysResult.getMarker());
		}
	}

	private void lastAccessInfo(String accessKeyId, User user) {
		GetAccessKeyLastUsedRequest accessKeyLastUsedRequest = new GetAccessKeyLastUsedRequest()
				.withAccessKeyId(accessKeyId);
		GetAccessKeyLastUsedResult accessKeyLastUsedResult = iamClient.getAccessKeyLastUsed(accessKeyLastUsedRequest);
		user.addAccessKeyInfo(accessKeyId, accessKeyLastUsedResult.getAccessKeyLastUsed().getLastUsedDate());
	}
	
	private void writeCSVReport(List<User> userList) {
		String fileName = buildFileName();
		Path path = Paths.get(System.getProperty("java.io.tmpdir"), "/", fileName);
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write("User Name, User Id, Access Key Id, Last used date time \n");
			userList.stream().filter(user -> !CollectionUtils.isEmpty(user.getAccessKeyInfoList())).forEach(user -> {
				user.getAccessKeyInfoList().forEach(e -> {
					try {
						writer.write(String.format("%s, %s, %s, %s \n", user.getUserName(), user.getUserId(),
								e.getAccessKeyId(), e.getLastUsedDate()));
					} catch (IOException e1) {
						LOGGER.error(e1.getMessage(), e1);
					}
				});
			});
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard().build();
		PutObjectResult putObjectResult = amazonS3.putObject(environment.getProperty("S3_BUCKET_NAME"), fileName,
				new File(System.getProperty("java.io.tmpdir") + "/" + fileName));

		LOGGER.info("PutObjectResult = {}", putObjectResult);
		
		
		try {
			Files.delete(path);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	private String buildFileName() {
		return "AccessKeyReport-" + LocalDateTime.now().format(dateTimeFormatter) + ".csv";
	}
}
