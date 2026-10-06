package io.sala.krob_krong.common.utils.s3;

import io.sala.krob_krong.common.utils.s3.dto.MultipartUpload;
import io.sala.krob_krong.common.utils.s3.dto.PartETag;
import io.sala.krob_krong.common.utils.s3.dto.PresignedPart;
import io.sala.krob_krong.common.utils.s3.dto.PresignedUrl;
import io.sala.krob_krong.common.utils.s3.dto.StoredObject;
import io.sala.krob_krong.common.utils.s3.dto.UploadedPart;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface S3Util {

    PresignedUrl presignUpload(String key, String contentType);

    PresignedUrl presignDownload(String key);

    MultipartUpload startMultipartUpload(String key, String contentType);

    List<PresignedPart> presignUploadParts(String key, String uploadId, int partCount);

    List<UploadedPart> listUploadedParts(String key, String uploadId);

    StoredObject completeMultipartUpload(String key, String uploadId, List<PartETag> parts);

    void abortMultipartUpload(String key, String uploadId);

    StoredObject upload(String key, InputStream content, String contentType);

    Optional<StoredObject> stat(String key);

    void delete(String key);
}
