# iam-user-monitor
This project contains lambda functions that helps around monitoring IAM users.

## Lambda Function - `AccessKeyLastUsedReportFunction`
This function will scan all IAM users from AWS account and creates CSV report containing IAM users which have active programmatic access enabled. CSV report contains IAM user name, user id, Access Key, Last used timestamp.

### Deployment

##### IAM Policy 
Creates IAM role which trusts `lambda.amazonaws.com` and has following IAM policy attached, apart from the normal policies that lambda environment needs.

<p align="center">
  
| Sr. No.  |      Action                         |  Resources                                                                                                                            |
|:--------:|:------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------- |
| 1        |    iam:ListUsers                    |  *                                                                                                                                    |
| 2        |    iam:ListAccessKeys               |  *                                                                                                                                    |
| 3        |    iam:GetAccessKeyLastUsed         |  *                                                                                                                                    |
| 4        |    s3:putObject                     |  S3 Bucket chosen for report storage																								     |

</p>

##### Environment variables

<p align="center">
  
| Sr. No.  |      Action                         |  Resources                                                                                                                            |
|:--------:|:------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------- |
| 1        |    FUNCTION_NAME                    |  AccessKeyLastUsedReportFunction                                                                                                      |                                                                                                                                   
| 2        |    S3_BUCKET_NAME                   |  S3 bucket name where you want lambda to upload the CSV report                                                                        |                                                                                                                                               

</p>