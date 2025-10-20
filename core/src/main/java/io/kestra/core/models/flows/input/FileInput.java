package io.kestra.core.models.flows.input;

import java.util.Set;
import io.kestra.core.models.flows.Input;
import io.kestra.core.validations.FileInputValidation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.List;

@SuperBuilder
@Getter
@NoArgsConstructor
@FileInputValidation
public class FileInput extends Input<URI> {

    private static final String DEFAULT_EXTENSION = ".upl";

    @Deprecated(since = "0.24", forRemoval = true)
    public String extension;
    
    /**
     * List of allowed file extensions (e.g., [".csv", ".txt", ".pdf"]).
     * Each extension must start with a dot.
     */
    private List<String> acceptedExtensions;
    
    /**
     * Gets the file extension from the URI's path
     */
    private String getFileExtension(URI uri) {
        String path = uri.getPath();
        int lastDotIndex = path.lastIndexOf(".");
        return lastDotIndex >= 0 ? path.substring(lastDotIndex).toLowerCase() : "";
    }

    @Override
    public void validate(URI input) throws ConstraintViolationException {
        if (input == null || acceptedExtensions == null || acceptedExtensions.isEmpty()) {
            return;
        }

        String extension = getFileExtension(input);
        if (!acceptedExtensions.contains(extension.toLowerCase())) {
            throw new ConstraintViolationException(
                "File type not allowed. Accepted extensions: " + String.join(", ", acceptedExtensions),
                Set.of()
            );
        }
    }

    public static String findFileInputExtension(@NotNull final List<Input<?>> inputs, @NotNull final String fileName) {
        return inputs.stream()
            .filter(in -> in instanceof FileInput)
            .filter(in -> in.getId().equals(fileName))
            .map(in -> (FileInput) in)
            .filter(fileInput -> fileInput.getAcceptedExtensions() != null && !fileInput.getAcceptedExtensions().isEmpty())
            .map(fileInput -> fileInput.getAcceptedExtensions().get(0))
            .findFirst()
            .orElse(FileInput.DEFAULT_EXTENSION);
    }
}
