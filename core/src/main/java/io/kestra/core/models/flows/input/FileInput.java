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
     * The allowed file extensions or MIME types.
     * Example: ".csv,.txt" or "text/csv,text/plain"
     * Can contain either file extensions starting with dots (.pdf, .txt) or MIME types (text/plain)
     */
    private String accept;
    
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
        if (input == null || accept == null || accept.isEmpty()) {
            return;
        }

        String extension = getFileExtension(input);
        String[] allowedTypes = accept.toLowerCase().split(",");
            
        boolean isValid = false;
        for (String type : allowedTypes) {
            type = type.trim();
            if (type.startsWith(".")) {
                // Extension validation
                if (extension.equals(type)) {
                    isValid = true;
                    break;
                }
            } else if (type.contains("/")) {
                // MIME type validation
                String fileExt = extension.substring(1); // Remove the dot
                if ((type.equals("text/csv") && fileExt.equals("csv")) ||
                    (type.equals("text/plain") && (fileExt.equals("txt") || fileExt.equals("text"))) ||
                    (type.equals("application/json") && fileExt.equals("json")) ||
                    (type.equals("text/markdown") && (fileExt.equals("md") || fileExt.equals("markdown"))) ||
                    (type.startsWith("image/") && fileExt.equals(type.substring(6)))) {
                    isValid = true;
                    break;
                }
            }
        }
            
        if (!isValid) {
            throw new ConstraintViolationException(
                "File type not allowed. Accepted types: " + accept,
                Set.of()
            );
        }
    }

    public static String findFileInputExtension(@NotNull final List<Input<?>> inputs, @NotNull final String fileName) {
        String res = inputs.stream()
            .filter(in -> in instanceof FileInput)
            .filter(in -> in.getId().equals(fileName))
            .filter(flowInput -> ((FileInput) flowInput).getExtension() != null)
            .map(flowInput -> ((FileInput) flowInput).getExtension())
            .findFirst()
            .orElse(FileInput.DEFAULT_EXTENSION);
        return res.startsWith(".") ? res : "." + res;
    }
}
