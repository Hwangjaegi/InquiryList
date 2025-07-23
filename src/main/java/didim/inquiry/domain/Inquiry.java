package didim.inquiry.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Inquiry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                //문의고유번호

    private String title;           //문의제목
    @Lob
    @Column(nullable = false , columnDefinition = "TEXT")
    private String content;         //문의내용
    private LocalDateTime createdAt; //생성날짜
    private String status;          //문의상태
    private String tickNumber;      //티켓번호

    @ManyToOne
    @JoinColumn(name = "writer_id" , nullable = false)
    private User writer;          //작성자 이름 또는 ID

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToMany(mappedBy = "inquiry" , cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now(); //문의작성 시 자동으로 현재날짜 주입
    }
}
