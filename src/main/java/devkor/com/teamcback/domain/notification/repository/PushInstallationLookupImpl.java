package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.annotation.Transactional;

@Transactional(transactionManager = "pushTransactionManager", readOnly = true)
public class PushInstallationLookupImpl implements PushInstallationLookup {
    private final PushMemberRepository members;
    private final EntityManager push;
    public PushInstallationLookupImpl(PushMemberRepository members,
            @Qualifier("pushEntityManagerFactory") EntityManagerFactory factory) {
        this.members = members;
        this.push = SharedEntityManagerCreator.createSharedEntityManager(factory);
    }
    public List<PushInstallationRepository.UserProfile> findUserProfiles(Collection<Long> ids) {
        List<PushInstallationRepository.UserProfile> result = new ArrayList<>();
        batches(ids).forEach(batch -> result.addAll(members.profiles(batch)));
        return result;
    }
    public Set<Long> findAdminUserIds(Collection<Long> ids) {
        Set<Long> result = new HashSet<>();
        batches(ids).forEach(batch -> result.addAll(members.admins(batch)));
        return result;
    }
    public List<PushInstallation> searchByUserProfile(String pattern, AppVariant variant, boolean adminOnly, Pageable page) {
        // Scan member IDs in bounded batches, retaining only the newest requested devices.
        int retain = Math.toIntExact(page.getOffset()) + page.getPageSize();
        List<PushInstallation> result = new ArrayList<>();
        long after = 0;
        Comparator<PushInstallation> newest = Comparator.comparing(PushInstallation::getModifiedAt,
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(PushInstallation::getPushInstallationId, Comparator.reverseOrder());
        while (true) {
            List<Long> ids = members.search(pattern, adminOnly, after, PageRequest.of(0, 500));
            if (ids.isEmpty()) break;
            result.addAll(push.createQuery("select i from PushInstallation i where i.userId in :ids and (:variant is null or i.appVariant = :variant) order by i.modifiedAt desc, i.pushInstallationId desc", PushInstallation.class)
                    .setParameter("ids", ids).setParameter("variant", variant).setMaxResults(retain).getResultList());
            result.sort(newest);
            if (result.size() > retain) result = new ArrayList<>(result.subList(0, retain));
            after = ids.get(ids.size()-1);
            if (ids.size() < 500) break;
        }
        return result.stream().skip(page.getOffset()).limit(page.getPageSize()).toList();
    }
    private List<List<Long>> batches(Collection<Long> ids) {
        List<Long> values = ids.stream().distinct().toList();
        List<List<Long>> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i += 500) result.add(values.subList(i, Math.min(i + 500, values.size())));
        return result;
    }
}
