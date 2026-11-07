package com.example.app.service;

import com.example.app.common.models.RespMsg;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface AttachmentService {

    ResponseEntity<ByteArrayResource> getAttachmentById(Integer projectId, Integer taskId,
                                                        Integer attachId, Long userId);

    RespMsg addAttachmentToTask(Integer projectId, Integer taskId,
                                    MultipartFile file, Long userId);

    RespMsg deleteAttachmentFromTask(Integer projectId, Integer taskId,
                                        Integer attachId, Long userId);

}
