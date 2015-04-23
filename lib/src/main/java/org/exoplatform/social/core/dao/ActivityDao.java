package org.exoplatform.social.core.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.entity.Activity;
import org.exoplatform.social.core.entity.Comment;
import org.exoplatform.social.core.entity.StreamItem;
import org.exoplatform.social.core.entity.StreamType;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.mysql.model.ActivityStreamEntity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage.TimestampType;
import org.exoplatform.social.core.storage.api.ActivityStreamStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;

/**
 * Created by bdechateauvieux on 4/18/15.
 */
public class ActivityDao {
  private static final Log LOG = ExoLogger.getLogger(ActivityDao.class);

  private final EntityManagerFactory FACTORY;
  private final RelationshipStorage relationshipStorage;
  private final IdentityStorage identityStorage;
  private final SpaceStorage spaceStorage;
  private ActivityStreamStorage streamStorage;
  private final static String ENTITY_MANAGER_KEY = "SOC_ENTITY_MANAGER";

  public ActivityDao(final RelationshipStorage relationshipStorage,
                     final IdentityStorage identityStorage,
                     final SpaceStorage spaceStorage,
                     final ActivityStreamStorage streamStorage) {
    FACTORY = Persistence.createEntityManagerFactory("org.exoplatform.social.hibernate-activity");
    //
    this.relationshipStorage = relationshipStorage;
    this.identityStorage = identityStorage;
    this.spaceStorage = spaceStorage;
    this.streamStorage = streamStorage;
  }

  public synchronized EntityManager getCurrentEntityManager() {
    EntityManager entityManager = (EntityManager) ConversationState.getCurrent().getAttribute(ENTITY_MANAGER_KEY);
    if (entityManager == null || !entityManager.isOpen() ||
        !entityManager.getTransaction().isActive()) {
      if(entityManager != null) {
        entityManager.close();
      }
      //
      entityManager = FACTORY.createEntityManager();
      entityManager.getTransaction().begin();
      ConversationState.getCurrent().setAttribute(ENTITY_MANAGER_KEY, entityManager);
    }
    return entityManager;
  }

  public synchronized void commit() {
    getCurrentEntityManager().getTransaction().commit();
  }

  private synchronized void saveEntity(Object entity) {
    try {
      getCurrentEntityManager().persist(entity);
      commit();
    } catch (Exception e) {
      LOG.error("Failed to create " + entity.getClass().getSimpleName(), e);
    }
  }

  private void updateEntity(Object entity) {
    try {
      entity = getCurrentEntityManager().merge(entity);
      commit();
    } catch (Exception e) {
      LOG.error("Failed to update " + entity.getClass().getSimpleName(), e);
    }
  }

  private void removeEntity(Object entity) {
    getCurrentEntityManager().remove(entity);
    commit();
  }

  public List<Activity> getActivityByLikerId(String likerId) {
    // TODO One activityManager per session
    TypedQuery<Activity> query = getCurrentEntityManager().createNamedQuery("getActivitiesByLikerId", Activity.class);
    query.setParameter("likerId", "1");
    return query.getResultList();
  }

  public Activity getActivity(String activityId) throws ActivityStorageException {
    return getCurrentEntityManager().find(Activity.class, activityId);
  }

  public List<Activity> getUserActivities(Identity owner) throws ActivityStorageException {
    return getUserActivities(owner, 0, -1);
  }

  public List<Activity> getUserActivities(Identity owner, long offset, long limit) throws ActivityStorageException {
    TypedQuery<Activity> query = getCurrentEntityManager().createNamedQuery("getUserActivities", Activity.class);
    query.setParameter("ownerId", owner.getId());
    if (limit > 0) {
      query.setFirstResult((int) offset);
      query.setMaxResults((int) limit);
    }
    return query.getResultList();
  }

  public List<Activity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    StringBuilder strQuery = new StringBuilder();
    strQuery.append("select new ")//DISTINCT
            .append(StreamItem.class.getName())
            .append("(activityId) from StreamItem as s join a.activityId Activity a where (s.viewerId = '")
            .append(viewer.getId())
            .append("') and (a.ownerId ='")
            .append(owner.getId())
            .append("')");

    TypedQuery<StreamItem> query = getCurrentEntityManager().createNamedQuery(strQuery.toString(), StreamItem.class);
    if (limit > 0) {
      query.setFirstResult((int) offset);
      query.setMaxResults((int) limit);
    }

    List<Activity> activities = new ArrayList<Activity>();
    List<StreamItem> streamItems = query.getResultList();
    for (StreamItem streamItem : streamItems) {
      activities.add(streamItem.getActivity());
    }
    return activities;
  }

  public Activity saveActivity(Identity owner, Activity activity) throws ActivityStorageException {
    String remoter = owner.getRemoteId();
    activity.setPosterId(activity.getOwnerId() != null ? activity.getOwnerId() : owner.getId());
    //
    ActivityStreamEntity stream = new ActivityStreamEntity();
    stream.setId(owner.getId());
    stream.setPrettyId(remoter);
    stream.setType(owner.getProviderId());
    //
//    stream.setActivity(activity);
//    activity.setActivityStream(stream);
    //
    saveEntity(activity);
    //
    saveStreamItem(owner, activity);
    //
    return activity;
  }

  private List<StreamItem> findStreamItemByActivityId(String activityId) {
    TypedQuery<StreamItem> query = getCurrentEntityManager().createNamedQuery("getStreamByActivityId", StreamItem.class);
    query.setParameter("activityId", activityId);
    return query.getResultList();
  }
  
  private void saveStreamItem(Identity poster, Activity activity) {
    //create StreamItem
    if (OrganizationIdentityProvider.NAME.equals(poster.getProviderId())) {
      //poster
      poster(poster, activity);
      //connection
      //connection(poster, activity);
      //mention
      mention(poster, activity);
    } else {
      //for SPACE
      spaceMembers(poster, activity);
    }
  }

  private void poster(Identity poster, Activity activity) {
    String viewerId = activity.getOwnerId() != null ? activity.getOwnerId() : poster.getId();
    createStreamItem(StreamType.POSTER, activity, viewerId);
  }
        
  /**
   * Creates StreamItem for each user who has mentioned on the activity
   * 
   * @param poster
   * @param activity
   * @throws MongoException
   */
  private void mention(Identity poster, Activity activity) {
    // calculate mentioners
    for (String mentioner : new String[]{}) {
      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, mentioner);
      if(identity != null) {
        createStreamItem(StreamType.MENTIONER, activity, identity.getId());
      }
    }
  }

  private void spaceMembers(Identity poster, Activity activity) {
    Space space = spaceStorage.getSpaceByPrettyName(poster.getRemoteId());

    if (space == null) return;
    //
    String viewerId = activity.getOwnerId() != null ? activity.getOwnerId() : poster.getId();
    createStreamItem(StreamType.SPACE_MEMBER, activity, viewerId);
  }

  private void createStreamItem(StreamType streamType, Activity activity, String viewerId){
    
    StreamItem streamItem = new StreamItem(streamType);
    streamItem.setViewerId(viewerId);
    streamItem.setActivity(activity);
    //
    saveEntity(streamItem);
  }

  public void saveComment(Activity activity, Comment comment) throws ActivityStorageException {
    activity.addComment(comment);
    updateActivity(activity);
  }

  public Activity getActivityByComment(Comment comment) throws ActivityStorageException {
    TypedQuery<Activity> query = getCurrentEntityManager().createNamedQuery("getActivityByComment", Activity.class);
    query.setParameter("COMMENT_ID", comment.getId());
    return query.getSingleResult();
  }

  public List<Comment> getNewerComments(Activity existingActivity, Long sinceTime, int limit) {
    return null;
  }

  public List<Comment> getOlderComments(Activity existingActivity, Long sinceTime, int limit) {
    return null;
  }

  public int getNumberOfNewerComments(Activity existingActivity, Long sinceTime) {
    return 0;
  }

  public int getNumberOfOlderComments(Activity existingActivity, Long sinceTime) {
    return 0;
  }

  public Comment getComment(Long commentId) throws ActivityStorageException {
    return getCurrentEntityManager().find(Comment.class, commentId);
  }

  public void deleteActivity(String activityId) throws ActivityStorageException {
    List<StreamItem> streamItems = findStreamItemByActivityId(activityId);
    for (StreamItem streamItem : streamItems) {
      removeEntity(streamItem);
    }
    //
    removeEntity(getActivity(activityId));
  }

  public void deleteComment(Comment comment) throws ActivityStorageException {
    removeEntity(comment);
  }

  public List<Activity> getActivitiesOfIdentities(List<Identity> connectionList, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  public List<Activity> getActivitiesOfIdentities(List<Identity> connectionList, TimestampType type, long offset, long limit) throws ActivityStorageException {
    return null;
  }

  public int getNumberOfUserActivities(Identity owner) throws ActivityStorageException {
    return 0;
  }

  public int getNumberOfUserActivitiesForUpgrade(Identity owner) throws ActivityStorageException {
    return 0;
  }

  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, Activity baseActivity) {
    return 0;
  }

  public List<Activity> getNewerOnUserActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnUserActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivityFeed(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivityFeedForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfActivitesOnActivityFeedForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnActivityFeed(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnActivityFeed(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesOfConnectionsForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfActivitiesOfConnectionsForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getActivitiesOfIdentity(Identity ownerIdentity, long offset, long limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity, long limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getUserSpacesActivitiesForUpgrade(Identity ownerIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfUserSpacesActivities(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUserSpacesActivitiesForUpgrade(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnUserSpacesActivities(Identity ownerIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Comment> getComments(Activity existingActivity) {
    return getComments(existingActivity, 0, -1);
  }

  public List<Comment> getComments(Activity existingActivity, int offset, int limit) {
    return existingActivity.getComments();
  }

  public int getNumberOfComments(Activity existingActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerComments(Activity existingActivity, Activity baseComment) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerComments(Activity existingActivity, Activity baseComment, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderComments(Activity existingActivity, Activity baseComment) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderComments(Activity existingActivity, Activity baseComment, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public SortedSet<ActivityProcessor> getActivityProcessors() {
    // TODO Auto-generated method stub
    return null;
  }

  public void updateActivity(Activity existingActivity) throws ActivityStorageException {
    updateEntity(existingActivity);
  }

  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getActivitiesOfIdentities(ActivityBuilderWhere where, ActivityFilter filter, long offset, long limit) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfSpaceActivitiesForUpgrade(Identity spaceIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getSpaceActivities(Identity spaceIdentity, int index, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getSpaceActivitiesForUpgrade(Identity spaceIdentity, int index, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getActivitiesByPoster(Identity posterIdentity, int offset, int limit, String... activityTypes) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfActivitiesByPoster(Identity posterIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfActivitiesByPoster(Identity ownerIdentity, Identity viewerIdentity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerOnSpaceActivities(Identity spaceIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getOlderOnSpaceActivities(Identity spaceIdentity, Activity baseActivity, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, Activity baseActivity) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    // TODO Auto-generated method stub
    return 0;
  }

  public List<Activity> getNewerFeedActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerUserActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getNewerSpaceActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderFeedActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderUserActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderUserSpacesActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderActivitiesOfConnections(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<Activity> getOlderSpaceActivities(Identity owner, Long sinceTime, int limit) {
    // TODO Auto-generated method stub
    return null;
  }

  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getNumberOfOlderOnSpaceActivities(Identity ownerIdentity, Long sinceTime) {
    // TODO Auto-generated method stub
    return 0;
  }
}