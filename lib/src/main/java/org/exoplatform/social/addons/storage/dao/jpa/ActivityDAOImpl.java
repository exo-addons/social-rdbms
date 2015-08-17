/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.storage.dao.jpa;

import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.jpa.query.AStreamQueryBuilder;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public class ActivityDAOImpl extends GenericDAOJPAImpl<Activity, Long> implements ActivityDAO {
  
  public List<Activity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException {
    return AStreamQueryBuilder.builder()
                              .owner(owner)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  public List<Activity> getActivityFeed(Identity ownerIdentity, int offset, int limit, long nbConnections, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  public int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity, long nbConnections, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .buildCount().getSingleResult().intValue();
        
  }
  
  @Override
  public List<Activity> getNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, int limit, long nbConnections, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .newer(sinceTime)
                              .ascOrder()
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  
  @Override
  public int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, long sinceTime, long nbConnections, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .newer(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<Activity> getOlderOnActivityFeed(Identity ownerIdentity, long sinceTime,int limit, long nbConnections, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .older(sinceTime)
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, long sinceTime, long nbConnections, List<String> spaceIds) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .memberOfSpaceIds(spaceIds)
                              .older(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<Activity> getUserActivities(Identity owner,
                                          long offset,
                                          long limit) throws ActivityStorageException {

    return AStreamQueryBuilder.builder()
                              .owner(owner)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();

  }
  
  @Override
  public int getNumberOfUserActivities(Identity ownerIdentity) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }
  
  @Override
  public List<Activity> getNewerOnUserActivities(Identity ownerIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .newer(sinceTime)
                              .ascOrder()
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();

  }

  @Override
  public int getNumberOfNewerOnUserActivities(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .newer(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<Activity> getOlderOnUserActivities(Identity ownerIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .older(sinceTime)
                              .offset(0)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnUserActivities(Identity ownerIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(ownerIdentity)
                              .older(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  public List<Activity> getSpaceActivities(Identity spaceOwner, long offset, long limit) throws ActivityStorageException {
    return AStreamQueryBuilder.builder()
                              .owner(spaceOwner)
                              .offset(offset)
                              .limit(limit)
                              .build()
                              .getResultList();
  }
  
  @Override
  public int getNumberOfSpaceActivities(Identity spaceIdentity) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }
  
  @Override
  public List<Activity> getNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .ascOrder()
                              .newer(sinceTime)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(Identity spaceIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .newer(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<Activity> getOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime, int limit) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .older(sinceTime)
                              .limit(limit)
                              .build()
                              .getResultList();
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(Identity spaceIdentity, long sinceTime) {
    return AStreamQueryBuilder.builder()
                              .owner(spaceIdentity)
                              .older(sinceTime)
                              .buildCount()
                              .getSingleResult()
                              .intValue();
  }

  @Override
  public List<Activity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .offset(offset)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }
  
  public int getNumberOfUserSpacesActivities(Identity ownerIdentity, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
  }
  
  @Override
  public List<Activity> getNewerOnUserSpacesActivities(Identity ownerIdentity,
                                                       long sinceTime,
                                                       int limit, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .newer(sinceTime)
                                .ascOrder()
                                .offset(0)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public int getNumberOfNewerOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .newer(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
  }

  @Override
  public List<Activity> getOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, int limit, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .older(sinceTime)
                                .offset(0)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(Identity ownerIdentity, long sinceTime, List<String> spaceIds) {
    if (spaceIds.size() > 0) {
      return AStreamQueryBuilder.builder()
                                .memberOfSpaceIds(spaceIds)
                                .older(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();
    } else {
      return 0;
    }
    
  }

  @Override
  public List<Activity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit, long nbConnections) {
    if (nbConnections > 0) {
      return AStreamQueryBuilder.builder()
          .offset(offset)
          .limit(limit)
          .build()
          .getResultList();
    } else {
      return Collections.emptyList();
    }
    
  }

  @Override
  public int getNumberOfActivitiesOfConnections(Identity ownerIdentity, long nbConnections) {
    if (nbConnections > 0) {
      return AStreamQueryBuilder.builder()
                                .buildCount()
                                .getSingleResult()
                                .intValue();

    } else {
      return 0;
    }
  }

  @Override
  public List<Activity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, long limit, long nbConnections) {
    if (nbConnections > 0) {
      return AStreamQueryBuilder.builder()
                                .newer(sinceTime)
                                .ascOrder()
                                .offset(0)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, long nbConnections) {
    if (nbConnections > 0) {
      return AStreamQueryBuilder.builder()
                                .newer(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();

    } else {
      return 0;
    }

  }

  @Override
  public List<Activity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, int limit, long nbConnections) {
    if (nbConnections > 0) {
      return AStreamQueryBuilder.builder()
                                .older(sinceTime)
                                .offset(0)
                                .limit(limit)
                                .build()
                                .getResultList();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, long sinceTime, long nbConnections) {
    if (nbConnections > 0) {
      return AStreamQueryBuilder.builder()
                                .older(sinceTime)
                                .buildCount()
                                .getSingleResult()
                                .intValue();

    } else {
      return 0;
    }
  }

}
