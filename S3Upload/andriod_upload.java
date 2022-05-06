/*
 *  代码参考:
 *  https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/get-started.html#get-started-setup-javamaven
 *  https://github.com/aws/aws-sdk-java-v2/blob/master/services/s3/src/it/java/software/amazon/awssdk/services/s3/S3PresignerIntegrationTest.java
 *
 */
package com.example.myapp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.function.Consumer;
import java.util.Random;

import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedAbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;

// demo2
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.List;


public class App {

    private static String generateRandomObjectKey() {
        return "s3-presigner-test-" + UUID.randomUUID();
    }

    private static Consumer<CreateMultipartUploadRequest.Builder> createMultipartUploadRequest(String bucketName, String objectKey) {
        return r -> r.bucket(bucketName).key(objectKey);
    }

    private static Consumer<CompleteMultipartUploadRequest.Builder> createMultipartUploadRequest(String bucketName, String objectKey, CreateMultipartUploadResponse create, CompletedMultipartUpload completedMultipartUpload) {
        return c -> c.bucket(bucketName)
                     .key(objectKey)
                     .uploadId(create.uploadId())
                     .multipartUpload(completedMultipartUpload);
    }

    private static HttpExecuteResponse execute(PresignedRequest presigned, String payload) throws IOException {
        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        ContentStreamProvider requestPayload = payload == null ? null : () -> new StringInputStream(payload);

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(presigned.httpRequest())
                                                       .contentStreamProvider(requestPayload)
                                                       .build();

        return httpClient.prepareRequest(request).call();
    }

    private static String fakeContent() {
        int length = 6 * 1024 * 1024; // 6M

        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            stringBuffer.append(str.charAt(number));
        }
        return stringBuffer.toString();
    }

    public static void main(String[] args) throws IOException {

        Region region = Region.AP_SOUTHEAST_1;
        //S3Client s3 = S3Client.builder().region(region).build();


        String bucket = "multiupload-presigned-demo-123";
        String objectKey = generateRandomObjectKey();

        //demo1(s3, bucket, objectKey, region);
        
        System.out.println("=====================================================");
        System.out.println("    **演示使用不合法的对象key进行分片上传**\n");
        demo2(bucket, "userid3/" + objectKey, region);
        System.out.println("=====================================================");
        System.out.println("    **以下演示对象key合法的分片上传**\n");
        demo2(bucket, "userid2/" + objectKey, region);
        System.out.println("=====================================================");


        //System.out.println("Closing the connection to Amazon S3");
        //s3.close();
        System.out.println("Connection closed");
        System.out.println("Exiting...");
        System.exit(1);
    }

    // multiupload with presigned url demo
    public static void demo1(S3Client client, String bucketName, String objectKey, Region region) {
        try {

            S3Presigner presigner = S3Presigner.create();

            /* demo string content for upload */
            String testObjectContent = fakeContent();

            /* how many parts to split */
            Integer parts = 2;

            CreateMultipartUploadResponse create = client.createMultipartUpload(createMultipartUploadRequest(bucketName, objectKey));

            // upload part 1
            PresignedUploadPartRequest uploadPart =
            presigner.presignUploadPart(up -> up.signatureDuration(Duration.ofDays(1))
                                                .uploadPartRequest(upr -> upr.bucket(bucketName)
                                                                             .key(objectKey)
                                                                             .partNumber(1)
                                                                             .uploadId(create.uploadId())));
            System.out.println("part 1 presigned url: " + uploadPart.url().toString());

            HttpExecuteResponse uploadPartResponse = execute(uploadPart, testObjectContent);
            System.out.println("httpResponse: " + uploadPartResponse.httpResponse().statusText());
            System.out.println("httpResponse is success: " + uploadPartResponse.httpResponse().isSuccessful());

            String etag1 = uploadPartResponse.httpResponse().firstMatchingHeader("ETag").orElse(null);
            CompletedPart part1 = CompletedPart.builder().partNumber(1).eTag(etag1).build();

            // upload part 2
            uploadPart = presigner.presignUploadPart(up -> up.signatureDuration(Duration.ofDays(1))
                                                .uploadPartRequest(upr -> upr.bucket(bucketName)
                                                                             .key(objectKey)
                                                                             .partNumber(2)
                                                                             .uploadId(create.uploadId())));
            System.out.println("part 2 presigned url: " + uploadPart.url().toString());

            uploadPartResponse = execute(uploadPart, testObjectContent);
            System.out.println("httpResponse: " + uploadPartResponse.httpResponse().statusText());
            System.out.println("httpResponse is success: " + uploadPartResponse.httpResponse().isSuccessful());

            String etag2 = uploadPartResponse.httpResponse().firstMatchingHeader("ETag").orElse(null);
            CompletedPart part2 = CompletedPart.builder().partNumber(2).eTag(etag2).build();

            // execute complete
            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(part1, part2)
                .build();

            PresignedCompleteMultipartUploadRequest presignedRequest =
            presigner.presignCompleteMultipartUpload(
                r -> r.signatureDuration(Duration.ofDays(1))
                      .completeMultipartUploadRequest(createMultipartUploadRequest(bucketName, objectKey, create, completedMultipartUpload)));

            System.out.println("complete presigned url: " + presignedRequest.url().toString());
            HttpExecuteResponse completeResponse = execute(presignedRequest, presignedRequest.signedPayload().get().asUtf8String());
            System.out.println("complete response: " + completeResponse.httpResponse().statusText());
            System.out.println("complete response: " + completeResponse.httpResponse().isSuccessful());
            System.out.println("complete response: " + completeResponse.httpResponse().statusCode());

            String body = IoUtils.toUtf8String(completeResponse.responseBody().get());
            System.out.println("complete response: " + body);


        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IO Error");
            System.exit(1);
        }

    }

    public static final String POLICY_DOCUMENT =
    "{" +
    "  \"Version\": \"2012-10-17\"," +
    "  \"Statement\": [" +
    "    {" +
    "        \"Effect\": \"Allow\"," +
    "        \"Action\": [" +
    "            \"s3:PutObject\"," +
    "            \"s3:AbortMultipartUpload\"," +
    "            \"s3:CompleteMultipartUpload\"," +
    "            \"s3:CreateMultipartUpload\"," +
    "            \"s3:UploadPart\"" +
    "       ]," +
    "       \"Resource\": [" +
    "            \"arn:aws:s3:::multiupload-presigned-demo-123/userid1/*\"," +
    "            \"arn:aws:s3:::multiupload-presigned-demo-123/userid2/*\"" +
    "       ]" +
    "    }" +
    "   ]" +
    "}";

    public static void demo2(String bucketName, String objectKey, Region region) {
        try {
            // server side
            // 服务端通过sts获取临时访问凭证
            // 这部分代码相对简单和独立，在生产中可以考虑放到lambda中执行
            StsClient stsClient = StsClient.builder().region(region).build();

            String roleArn = "arn:aws:iam::911329921905:role/clientAccessDemoRole";
            String roleSessionName = "clientAccessDemoRole";

            AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .roleSessionName(roleSessionName)
                .policy(POLICY_DOCUMENT) // 附加访问权限，用来对本次访问进行精细化管理
                .durationSeconds(3600) // 设置失效时间 3600秒
                .build();

            AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);
            Credentials myCreds = roleResponse.credentials();

            Instant exTime = myCreds.expiration();
            String sessToken = myCreds.sessionToken();
            String accessKey = myCreds.accessKeyId();
            String secretAccessKey = myCreds.secretAccessKey();

            /*
            DateTimeFormatter formatter =
                   DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
                           .withLocale( Locale.US)
                           .withZone( ZoneId.systemDefault() );

            formatter.format( exTime );
            System.out.println("accessKey: " + accessKey);
            System.out.println("secretAccessKey: " + secretAccessKey);
            System.out.println("The token "+ sessToken + "  expires on " + exTime );
            System.out.println("");
            */
            System.out.println("# 服务端获取临时访问凭证信息：");
            System.out.println("   " + myCreds.toString());

            // 把 accessKey, secretAccessKey, sessionToken 返回给客户端后，服务端任务完成
            stsClient.close();

            // ===========================================
            // client side
            // 以下模拟客户端在收到临时访问凭证后的分片上传动作
            AwsSessionCredentials awsCreds = AwsSessionCredentials.create(
                accessKey, secretAccessKey, sessToken);

            S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

            System.out.println("# 客户端通过临时凭证构建S3客户端进行文件上传");
            System.out.println("  upload file to => ");
            System.out.println("    BUCKET: " + bucketName);
            System.out.println("       KEY: " + objectKey);

            // init multipart upload
            CreateMultipartUploadResponse create = s3.createMultipartUpload(createMultipartUploadRequest(bucketName, objectKey));

            // upload 2 parts
            int partNum;
            int partCount = 2;
            String uploadId = create.uploadId();
            List<UploadPartResponse> uploadPartResponses = new ArrayList<>();

            for (partNum = 1; partNum < (partCount + 1); partNum++) {
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(objectKey)
                                                            .uploadId(uploadId)
                                                            .partNumber(partNum)
                                                            .build();

                String content = fakeContent();
                RequestBody reqBody = RequestBody.fromString(content);
                uploadPartResponses.add(s3.uploadPart(uploadPartRequest, reqBody));
            }

            // complete multipart upload
            List<CompletedPart> completedParts = new ArrayList<>();

            for (int i = 0; i < uploadPartResponses.size(); i++) {
                partNum = i + 1;
                UploadPartResponse response = uploadPartResponses.get(i);
                completedParts.add(CompletedPart.builder().eTag(response.eTag()).partNumber(partNum).build());
            }

            CompleteMultipartUploadRequest completeRequest
                = CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder()
                                            .parts(completedParts)
                                            .build())
                        .build();

            CompleteMultipartUploadResponse completeMultipartUploadResponse =
                s3.completeMultipartUpload(completeRequest);
            System.out.println("   " + completeMultipartUploadResponse.toString());
            System.out.println("# 上传完成");

        } catch (S3Exception e) {
            System.err.println("  S3 Operation Error: " + e.awsErrorDetails().errorMessage());
        } catch (StsException e) {
            System.err.println("  STS Operation Error: " + e.getMessage());
        }
    }

}
