package com.transformco.hs.usermonitor.iamusermonitor.vo;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
	private String userId;
	private String userName;
	private final List<AccessKeyInfo> accessKeyInfoList = new ArrayList<>();
	
	public void addAccessKeyInfo(String accessKeyId, Date lastAccessedDateTime) {
		AccessKeyInfo accessKeyInfo = new AccessKeyInfo();
		accessKeyInfo.setAccessKeyId(accessKeyId);
		if (lastAccessedDateTime != null) {
			accessKeyInfo.setLastUsedDate(ZonedDateTime.ofInstant(lastAccessedDateTime.toInstant(), ZoneId.of("America/Chicago")));
		}
		accessKeyInfoList.add(accessKeyInfo);
	}
}