package devkor.com.teamcback.domain.vote.service;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.domain.vote.dto.request.SaveVoteRecordReq;
import devkor.com.teamcback.domain.vote.dto.response.GetVoteOptionRes;
import devkor.com.teamcback.domain.vote.dto.response.GetVoteRes;
import devkor.com.teamcback.domain.vote.dto.response.SaveVoteRecordRes;
import devkor.com.teamcback.domain.vote.entity.VoteOption;
import devkor.com.teamcback.domain.vote.entity.VoteRecord;
import devkor.com.teamcback.domain.vote.entity.VoteStatus;
import devkor.com.teamcback.domain.vote.entity.VoteTopic;
import devkor.com.teamcback.domain.vote.repository.VoteOptionRepository;
import devkor.com.teamcback.domain.vote.repository.VoteRecordRepository;
import devkor.com.teamcback.domain.vote.repository.VoteTopicRepository;
import devkor.com.teamcback.global.annotation.UpdateScore;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static devkor.com.teamcback.domain.vote.entity.VoteStatus.CLOSED;
import static devkor.com.teamcback.global.response.ResultCode.*;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final PlaceRepository placeRepository;
    private final VoteTopicRepository voteTopicRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteRecordRepository voteRecordRepository;

    /**
     * 투표 리스트 조회
     * @param status 투표 상태
     * @return
     */
    public List<GetVoteRes> getVoteList(VoteStatus status) {
        List<GetVoteRes> voteResList = new ArrayList<>();

        List<VoteTopic> voteTopicList = voteTopicRepository.findAllByStatus(status);

        for(VoteTopic voteTopic : voteTopicList) {
            List<VoteOption> voteOptionList = voteOptionRepository.findAllByVoteTopicId(voteTopic.getId());

            List<GetVoteOptionRes> voteOptionResList = new ArrayList<>();
            for(VoteOption voteOption : voteOptionList) {
                int voteCount = voteRecordRepository.countByVoteTopicIdAndVoteOptionId(voteTopic.getId(), voteOption.getId());
                voteOptionResList.add(new GetVoteOptionRes(voteOption.getId(), voteOption.getOptionText(), voteCount));
            }

            voteResList.add(new GetVoteRes(voteTopic.getId(), voteTopic.getTopic(), voteTopic.getStatus().getName(), voteOptionResList));
        }

        return voteResList;
    }

    /**
     * 투표 조회
     * @param voteTopicId 투표 주제 ID
     * @return
     */
    public GetVoteRes getVote(Long voteTopicId) {

        VoteTopic voteTopic = findVoteTopic(voteTopicId);

        List<VoteOption> voteOptionList = voteOptionRepository.findAllByVoteTopicId(voteTopic.getId());

        List<GetVoteOptionRes> voteOptionResList = new ArrayList<>();
        for(VoteOption voteOption : voteOptionList) {
            int voteCount = voteRecordRepository.countByVoteTopicIdAndVoteOptionId(voteTopicId, voteOption.getId());
            voteOptionResList.add(new GetVoteOptionRes(voteOption.getId(), voteOption.getOptionText(), voteCount));
        }

        return new GetVoteRes(voteTopic.getId(), voteTopic.getTopic(), voteTopic.getStatus().getName(), voteOptionResList);
    }

    /**
     * 투표 조회
     * @param placeId 장소 ID
     * @return
     */
    public GetVoteRes getVoteByPlace(Long voteTopicId, Long placeId) {

        VoteTopic voteTopic = findVoteTopic(voteTopicId);

        List<VoteOption> voteOptionList = voteOptionRepository.findAllByVoteTopicId(voteTopic.getId());

        List<GetVoteOptionRes> voteOptionResList = new ArrayList<>();
        for(VoteOption voteOption : voteOptionList) {
            int voteCount = voteRecordRepository.countByVoteTopicIdAndVoteOptionIdAndPlaceId(voteTopicId, voteOption.getId(), placeId);
            voteOptionResList.add(new GetVoteOptionRes(voteOption.getId(), voteOption.getOptionText(), voteCount));
        }
        return new GetVoteRes(voteTopic.getId(), voteTopic.getTopic(), voteTopic.getStatus().getName(), voteOptionResList);
    }


    /**
     * 투표 저장
     */
    @UpdateScore(addScore = 5)
    @Transactional
    public SaveVoteRecordRes saveVoteRecord(Long userId, SaveVoteRecordReq req) {
        if(userId == null) throw new GlobalException(FORBIDDEN);

        VoteOption voteOption = findVoteOption(req.getVoteOptionId());

        // 투표 주제의 옵션이 아닌 경우
        if(!Objects.equals(voteOption.getVoteTopicId(), req.getVoteTopicId())) throw new GlobalException(INVALID_INPUT);

        // 투표 주제 상태 확인
        VoteTopic voteTopic = findVoteTopic(req.getVoteTopicId());
        if(voteTopic.getStatus() == CLOSED) throw new GlobalException(CLOSED_VOTE);

        // 투표 장소 확인
        Place place = findPlace(req.getPlaceId());

        // 투표 이력 확인
        VoteRecord voteRecord = voteRecordRepository.findByUserIdAndVoteTopicIdAndPlaceId(userId, req.getVoteTopicId(), req.getPlaceId());
        if(voteRecord == null) { // 다른 거 선택한 경우 새로 저장
            voteRecordRepository.save(new VoteRecord(userId, req.getVoteTopicId(), req.getVoteOptionId(), req.getPlaceId()));
        }
        else if(!voteRecord.getVoteOptionId().equals(voteOption.getId())){ // 다른 걸 투표한 경우
            voteRecord.setVoteOptionId(req.getVoteOptionId());
        }
        else { // 같은 걸 투표한 경우 -> 토글로 취소됨
            voteRecord.setVoteOptionId(null); // 기존 투표 옵션을 null로 설정 (기록 남기기)
        }

        // 투표 종료 검사 로직
        List<VoteOption> optionList = voteOptionRepository.findAllByVoteTopicId(req.getVoteTopicId());
        if(optionList.isEmpty()) return new SaveVoteRecordRes();

        Integer minCount = null;
        Integer maxCount = null;
        Long maxOption = null;
        for(VoteOption option : optionList) {
            int count = voteRecordRepository.countByVoteTopicIdAndVoteOptionIdAndPlaceId(req.getVoteTopicId(), option.getId(), req.getPlaceId());
            if(minCount == null) {
                minCount = maxCount = count;
                maxOption = option.getId();
            }

            else if(count < minCount) {
                minCount = count;
            }
            else if(count > maxCount) {
                maxCount = count;
                maxOption = option.getId();
            }
        }

        if(maxCount >= 5 && maxCount >= minCount * 2) {
            // voteTopic.setStatus(CLOSED);
        }

        return new SaveVoteRecordRes();
    }

    private VoteTopic findVoteTopic(Long voteTopicId) {
        return voteTopicRepository.findById(voteTopicId).orElseThrow(() -> new GlobalException(NOT_FOUND_VOTE));
    }

    private VoteOption findVoteOption(Long voteOptionId) {
        return voteOptionRepository.findById(voteOptionId).orElseThrow(() -> new GlobalException(NOT_FOUND_VOTE_OPTION));
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }
}
