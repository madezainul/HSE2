package ahqpck.hse.safety.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC Configuration for static resources.
 * Configures resource handlers for serving uploaded files.
 */
@Configuration
public class WebConfigTrash implements WebMvcConfigurer {

    @Autowired
    private FileStoragePropertiesTrash fileStorageProperties;

    /**
     * Configure resource handlers for static file serving.
     * Maps /uploads/** requests to the uploads directory.
     * 
     * @param registry ResourceHandlerRegistry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get the configured upload directory
        String uploadDirPath = fileStorageProperties.getUploadDir();
        
        // Ensure path is absolute
        Path uploadsPath = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        String uploadsDir = uploadsPath.toUri().toString();
        
        System.out.println("Configured uploads directory: " + uploadDirPath);
        System.out.println("Absolute uploads path: " + uploadsPath);
        System.out.println("URI for resource handler: " + uploadsDir);
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsDir)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver());
    }
}
