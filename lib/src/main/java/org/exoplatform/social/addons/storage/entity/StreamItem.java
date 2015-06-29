package org.exoplatform.social.addons.storage.entity;

import javax.persistence.*;

import org.exoplatform.social.core.storage.query.PropertyLiteralExpression;

/**
 * Created by bdechateauvieux on 3/26/15.
 */
@Entity
@Table(name = "SOC_STREAM_ITEMS")
@NamedQuery(name = "getStreamByActivityId", query = "select s from StreamItem s join s.activity A where A.id = :activityId")
public class StreamItem {
  @Id
  @GeneratedValue
  @Column(name = "STREAM_ITEM_ID")
  private Long id;

  @OneToOne
  @JoinColumn(name = "ACTIVITY_ID")
  private Activity activity;

  /**
   * This is id's Identity owner of ActivityStream or SpaceStream
   */
  @Column(length = 36)
  private String ownerId;

  @Enumerated
  private StreamType streamType;

  public StreamItem() {
  }

  public StreamItem(StreamType streamType) {
    this.streamType = streamType;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Activity getActivity() {
    return activity;
  }

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public StreamType getStreamType() {
    return streamType;
  }

  public void setStreamType(StreamType streamType) {
    this.streamType = streamType;
  }

}