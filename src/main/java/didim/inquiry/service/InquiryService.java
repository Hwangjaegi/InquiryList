package didim.inquiry.service;

import didim.inquiry.domain.Inquiry;
import didim.inquiry.dto.SearchInquiryDto;
import didim.inquiry.repository.InquiryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public InquiryService(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    public void saveInquiry(Inquiry inquiry){
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);
        String lastTick = inquiryRepository.findLastTickNumber(year);
        System.out.println("티켓번호 : " + lastTick);

        int nextSeq = 1;
        if(lastTick != null && lastTick.length() >= 7){
            nextSeq = Integer.parseInt(lastTick.substring(2))+1;
        }

        String newTick = year + String.format("%05d",nextSeq);  // 년도 + 인덱스숫자
        inquiry.setTickNumber(newTick);

        inquiryRepository.save(inquiry);
    }

    //권한 어드민일 경우 전체 조회
    public Page<Inquiry> getInquiriesAllWithAnswers(Pageable pageable){
        return inquiryRepository.findAllWithAnswers(pageable);
    }

    //유저 아이디로 문의조회 -> inquiry도메인에 User Writer필드가있고 이는 User에 username필드가 있으면 이를 연관해 Id를 참조하여 조회
    public List<Inquiry> getInquiriesByUsername(String username){
        return inquiryRepository.findByWriter_Username(username);
    }

    //유저 아이디로 문의조회 -> inquiry도메인에 User Writer필드가있고 이는 User에 username필드가 있으면 이를 연관해 Id를 참조하여 조회
    public Page<Inquiry> getInquiriesByAnswersByUsername(String username , Pageable pageable){
//        return inquiryRepository.findWithAnswersByUsername(username);
        return inquiryRepository.findInquiriesByAnswersWithUserByUsername(username,pageable);
    }

    //문의 id로 문의 조회
    public Optional<Inquiry> getInquiryById(Long id){
        return inquiryRepository.findById(id);
    }

    //문의 id로 해당 문의 조회 후 상태 변경
    @Transactional
    public void updateInquiryStatus(Long id){
        Inquiry inquiry = inquiryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 문의가 없습니다 . Id = " + id));
        inquiry.setStatus("처리완료");
    }

    //검색을 통한 조회 처리 (어드민일경우 , 일반유저일경우)
    public Page<Inquiry> getInquiryBySearch(SearchInquiryDto searchInquiry, String role, String username , Pageable pageable) {
        List<String> statuses = searchInquiry.getStatus();
        if (statuses == null || statuses.isEmpty()) {
            statuses = null;
        }

        LocalDateTime start = null;
        LocalDateTime end = null;

        String yearMonthStr = searchInquiry.getYearMonth(); // "2024-07" 형식
        if (yearMonthStr != null && !yearMonthStr.isBlank()) {
            YearMonth yearMonth = YearMonth.parse(yearMonthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            start = yearMonth.atDay(1).atStartOfDay(); // 2024-07-01T00:00
            end = yearMonth.atEndOfMonth().atTime(23, 59, 59); // 2024-07-31T23:59:59
        }

        if ("ADMIN".equals(role)) {
            return inquiryRepository.findInquiriesBySearchWithAdmin(
                    searchInquiry.getTitle(),
                    searchInquiry.getWriter(),
                    start,
                    end,
                    statuses,
                    pageable
            );
        } else {
            return inquiryRepository.findInquiriesBySearchWithUserByUsername(
                    searchInquiry.getTitle(),
                    searchInquiry.getWriter(),
                    start,
                    end,
                    statuses,
                    username,
                    pageable
            );
        }
    }

    public Page<Inquiry> searchInquiries(String keyword, String yearMonth, List<String> statuses, String role, String username, Pageable pageable) {
        if (statuses == null || statuses.isEmpty()) {
            statuses = null;
        }
        if (keyword != null && !keyword.isBlank()) {
            keyword = keyword.trim();
        } else {
            keyword = null;
        }
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (yearMonth != null && !yearMonth.isBlank()) {
            java.time.YearMonth ym = java.time.YearMonth.parse(yearMonth, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            start = ym.atDay(1).atStartOfDay();
            end = ym.atEndOfMonth().atTime(23, 59, 59);
        }
        if ("ADMIN".equals(role)) {
            return inquiryRepository.findInquiriesByKeywordForAdmin(keyword, statuses, start, end, pageable);
        } else {
            return inquiryRepository.findInquiriesByKeywordForUser(keyword, statuses, start, end, username, pageable);
        }
    }
}
