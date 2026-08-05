package com.triptrace.domain.image.image.storage;

import com.triptrace.domain.image.image.processing.dto.StoredFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage localFileStorage;
    private final byte[] file = "TripTrace image".getBytes(StandardCharsets.UTF_8);
    private final String fileName = "image.jpg";

    @BeforeEach
    void setUp() {
        localFileStorage = new LocalFileStorage();
    }

    // save
    // given: 정상적인 파일, 없는 경로, 파일명
    // when: 실제 폴더를 생성하고 파일을 생성한다.
    // then: 실제 파일을 생성한다.
    @Test
    @DisplayName("없는 경로에 저장하면 디렉터리와 파일을 생성한다")
    void save_createsMissingDirectoryAndFile() throws IOException {
        Path missingDirectory = tempDir.resolve("images/serving");

        StoredFile storedFile = localFileStorage.save(file, missingDirectory.toString(), fileName);

        Path savedFile = missingDirectory.resolve(fileName);
        assertThat(Files.isDirectory(missingDirectory)).isTrue();
        assertThat(Files.readAllBytes(savedFile)).isEqualTo(file);
        assertThat(storedFile.getPath()).isEqualTo(missingDirectory.toString());
        assertThat(storedFile.getName()).isEqualTo(fileName);
        assertThat(storedFile.getSize()).isEqualTo((long) file.length);
    }

    // given: 정상적인 파일, 있는 경로, 파일명
    // when: 실제 폴더를 별도로 생성하지 않고, 파일을 생성한다.
    // then 실제로 파일을 생성한다.
    @Test
    @DisplayName("존재하는 경로에 저장하면 파일을 생성한다")
    void save_createsFileInExistingDirectory() throws IOException {
        Path existingDirectory = Files.createDirectory(tempDir.resolve("images"));

        localFileStorage.save(file, existingDirectory.toString(), fileName);

        assertThat(Files.readAllBytes(existingDirectory.resolve(fileName))).isEqualTo(file);
    }

    // delete
    // given: 없는 경로를 넘긴다.
    // when: delete
    // then: 예외를 반환하지 않고 처리한다.
    @Test
    @DisplayName("존재하지 않는 디렉터리의 파일을 삭제해도 예외가 발생하지 않는다")
    void delete_doesNotThrowWhenDirectoryDoesNotExist() {
        Path missingFile = tempDir.resolve("missing/path/image.jpg");

        assertThatCode(() -> localFileStorage.delete(missingFile.toString()))
            .doesNotThrowAnyException();
    }

    // given: 없는 파일의 경로를 넘긴다.
    // when: delete
    // then: 예외를 반환하지 않고 처리한다.
    @Test
    @DisplayName("존재하는 디렉터리의 없는 파일을 삭제해도 예외가 발생하지 않는다")
    void delete_doesNotThrowWhenFileDoesNotExist() throws IOException {
        Path existingDirectory = Files.createDirectory(tempDir.resolve("images"));

        assertThatCode(() -> localFileStorage.delete(existingDirectory.resolve(fileName).toString()))
            .doesNotThrowAnyException();
    }

    // given: 있는 파일 경로를 넘긴다.
    // when: delete
    // then: 실제 파일로 존재하지 않는다.
    @Test
    @DisplayName("존재하는 파일을 삭제하면 실제 파일이 사라진다")
    void delete_removesExistingFile() throws IOException {
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, file);

        localFileStorage.delete(filePath.toString());

        assertThat(Files.exists(filePath)).isFalse();
    }
}
