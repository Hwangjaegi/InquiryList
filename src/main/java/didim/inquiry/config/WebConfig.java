package didim.inquiry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload}")
    private String uploadDir;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // UTF-8 인코딩 설정
        restTemplate.getMessageConverters().add(0, 
            new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        return restTemplate;
    }

    // 개발자 모드에서 로그아웃 후 로그인시 에러 방지용 /.well-known 매핑
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);

        // 업로드된 temp 이미지 노출 (/temp/{filename} 으로 접근 가능)
        Path tempPath = Paths.get(uploadDir, "temp").toAbsolutePath().normalize();
        String tempLocation = tempPath.toUri().toString(); // ex: file:///Users/admin/Customer/upload/temp/
        registry.addResourceHandler("/temp/**")
                .addResourceLocations(tempLocation)
                .setCachePeriod(3600); // 캐시 필요에 따라 조정

        // 업로드된 temp 이미지 노출 (/temp/{filename} 으로 접근 가능)
        Path postsPath = Paths.get(uploadDir, "posts").toAbsolutePath().normalize();
        String postsLocation = postsPath.toUri().toString(); // ex: file:///Users/admin/Customer/upload/temp/
        registry.addResourceHandler("/posts/**")
                .addResourceLocations(postsLocation)
                .setCachePeriod(3600); // 캐시 필요에 따라 조정
    }
}
