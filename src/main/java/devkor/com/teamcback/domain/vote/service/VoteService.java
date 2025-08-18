package devkor.com.teamcback.domain.vote.service;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
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
import devkor.com.teamcback.global.exception.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            List<GetVoteOptionRes> voteOptionList = voteOptionRepository.findAllByVoteTopicId(voteTopic.getId())
                    .stream().map(option -> new GetVoteOptionRes(option.getId(), option.getOptionText(), option.getVoteCount())).toList();

            voteResList.add(new GetVoteRes(voteTopic.getId(), voteTopic.getTopic(), voteOptionList));
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

        List<GetVoteOptionRes> voteOptionList = voteOptionRepository.findAllByVoteTopicId(voteTopic.getId())
                .stream().map(option -> new GetVoteOptionRes(option.getId(), option.getOptionText(), option.getVoteCount())).toList();


        return new GetVoteRes(voteTopic.getId(), voteTopic.getTopic(), voteOptionList);
    }

    @Transactional
    public SaveVoteRecordRes saveVoteRecord(Long userId, Long voteTopicId, Long voteOptionId, Long placeId) {
        if(userId == null) throw new GlobalException(FORBIDDEN);

        VoteOption voteOption = findVoteOption(voteOptionId);

        // 투표 주제의 옵션이 아닌 경우
        if(!Objects.equals(voteOption.getVoteTopicId(), voteTopicId)) throw new GlobalException(INVALID_INPUT);

        // 투표 주제 상태 확인
        VoteTopic voteTopic = findVoteTopic(voteTopicId);
        if(voteTopic.getStatus() == CLOSED) throw new GlobalException(CLOSED_VOTE);

        // 투표 장소 확인
        if(placeId != null) {
            findPlace(placeId);
        }

        // 투표 이력 확인
        VoteRecord voteRecord = voteRecordRepository.findByUserIdAndVoteTopicIdAndVoteOptionIdAndPlaceId(userId, voteTopicId, voteOptionId, placeId);
        if(voteRecord == null) {
            voteRecordRepository.save(new VoteRecord(userId, voteTopicId, voteOptionId, placeId));
            voteOption.setVoteCount(voteOption.getVoteCount() + 1);
        }
        else {
            voteRecordRepository.deleteById(voteRecord.getId());
            voteOption.setVoteCount(voteOption.getVoteCount() - 1);
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
