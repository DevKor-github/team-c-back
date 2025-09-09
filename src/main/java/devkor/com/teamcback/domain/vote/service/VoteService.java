package devkor.com.teamcback.domain.vote.service;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.vote.dto.request.SaveVoteRecordReq;
import devkor.com.teamcback.domain.vote.dto.response.ChangeVoteStatusRes;
import devkor.com.teamcback.domain.vote.dto.response.GetVoteOptionRes;
import devkor.com.teamcback.domain.vote.dto.response.GetVoteRes;
import devkor.com.teamcback.domain.vote.dto.response.SaveVoteRecordRes;
import devkor.com.teamcback.domain.vote.entity.*;
import devkor.com.teamcback.domain.vote.repository.VoteOptionRepository;
import devkor.com.teamcback.domain.vote.repository.VoteRecordRepository;
import devkor.com.teamcback.domain.vote.repository.VoteRepository;
import devkor.com.teamcback.domain.vote.repository.VoteTopicRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static devkor.com.teamcback.global.response.ResultCode.*;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final PlaceRepository placeRepository;
    private final VoteTopicRepository voteTopicRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRecordRepository voteRecordRepository;
    private final VoteRepository voteRepository;

    /**
     * 장소별 투표 조회
     * @param voteTopicId 투표 주제 ID
     * @param placeId 장소 ID
     */
    @Transactional(readOnly = true)
    public GetVoteRes getVoteByPlace(Long voteTopicId, Long placeId) {

        // 투표 주제
        VoteTopic voteTopic = findVoteTopic(voteTopicId);

        // 투표
        Vote vote = findVote(voteTopicId, placeId);

        // 장소
        Place place = findPlace(placeId);

        // 투표 항목, 현황
        List<GetVoteOptionRes> voteOptionList = voteOptionRepository.getVoteOptionsByPlaceByVoteTopicIdAndPlaceId(voteTopic.getId(), placeId);

        return new GetVoteRes(vote.getId(), vote.getStatus().getName(), voteTopic.getId(), voteTopic.getTopic(), place.getId(), place.getName(), voteOptionList);
    }


    /**
     * 투표 저장
     */
    @UpdateScore(addScore = 2)
    @Transactional
    public SaveVoteRecordRes saveVoteRecord(Long userId, SaveVoteRecordReq req) {
        if(userId == null) throw new GlobalException(FORBIDDEN);

        // 투표
        Vote vote = findVote(req.getVoteId());

        // 투표한 옵션
        VoteOption voteOption = findVoteOption(req.getVoteOptionId());

        // 투표 주제의 옵션이 아닌 경우
        if(!Objects.equals(vote.getVoteTopicId(), voteOption.getVoteTopicId())) throw new GlobalException(INVALID_INPUT);

        // 투표 상태 확인
        vote.checkStatus();

        // 투표 이력 확인
        VoteRecord voteRecord = voteRecordRepository.findByUserIdAndVoteId(userId, vote.getId());
        if(voteRecord == null) { // 다른 거 선택한 경우 새로 저장
            voteRecordRepository.save(new VoteRecord(userId, vote.getId(), req.getVoteOptionId()));
        }
        else if(!Objects.equals(voteRecord.getVoteOptionId(), voteOption.getId())){ // 다른 걸 투표한 경우
            voteRecord.setVoteOptionId(req.getVoteOptionId());
        }
        else { // 같은 걸 투표한 경우 -> 토글로 취소됨
            voteRecord.setVoteOptionId(null); // 기존 투표 옵션을 null로 설정 (기록 남기기)
        }

        return new SaveVoteRecordRes();
    }

    /**
     * 투표 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<GetVoteRes> getVoteList(VoteStatus status, Long lastVoteId, int size) {
        List<GetVoteRes> resList = new ArrayList<>();

        List<Vote> voteList = voteOptionRepository.getVoteByStatusWithPage(status, lastVoteId, size);

        for(Vote vote : voteList) {
            // 투표 주제
            VoteTopic voteTopic = findVoteTopic(vote.getVoteTopicId());

            // 투표 장소
            Place place = findPlace(vote.getPlaceId());

            // 투표 항목, 현황
            List<GetVoteOptionRes> voteOptionList = voteOptionRepository.getVoteOptionByVoteId(vote.getId());

            resList.add(new GetVoteRes(vote.getId(), vote.getStatus().toString(), voteTopic.getId(), voteTopic.getTopic(), place.getId(), place.getName(), voteOptionList));
        }

        return resList;
    }

    /**
     * 투표 상태 변경
     */
    public ChangeVoteStatusRes changeVoteStatus(Long voteId, VoteStatus status) {
        // 투표
        Vote vote = findVote(voteId);

        // 상태 변경
        vote.changeStatus(status);

        return new ChangeVoteStatusRes();
    }

    private Vote findVote(Long voteTopicId, Long placeId) {
        return voteRepository.findByVoteTopicIdAndPlaceId(voteTopicId, placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_VOTE));
    }

    private Vote findVote(Long voteId) {
        return voteRepository.findById(voteId).orElseThrow(() -> new GlobalException(NOT_FOUND_VOTE));
    }

    private VoteTopic findVoteTopic(Long voteTopicId) {
        return voteTopicRepository.findById(voteTopicId).orElseThrow(() -> new GlobalException(NOT_FOUND_VOTE_TOPIC));
    }

    private VoteOption findVoteOption(Long voteOptionId) {
        return voteOptionRepository.findById(voteOptionId).orElseThrow(() -> new GlobalException(NOT_FOUND_VOTE_OPTION));
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }
}
