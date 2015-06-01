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
package org.exoplatform.social.addons.storage.dao.jpa.query;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.Activity_;
import org.exoplatform.social.addons.storage.entity.StreamItem;
import org.exoplatform.social.addons.storage.entity.StreamItem_;
import org.exoplatform.social.addons.storage.entity.StreamType;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 1, 2015  
 */
public final class AStreamQueryBuilder {
  private Identity owner;
  private long offset;
  private long limit;
  //newer or older
  private long sinceTime = 0;
  private boolean isNewer = false;
  //streamType
  private StreamType type = null;
  private boolean equalType;
  
  //order by
  private boolean descOrder = true;
  
  public static AStreamQueryBuilder builder() {
    return new AStreamQueryBuilder();
  }
  
  public AStreamQueryBuilder owner(Identity owner) {
    this.owner = owner;
    return this;
  }
  
  public AStreamQueryBuilder offset(long offset) {
    this.offset = offset;
    return this;
  }
  
  public AStreamQueryBuilder limit(long limit) {
    this.limit = limit;
    return this;
  }
  
  
  public AStreamQueryBuilder newer(long sinceTime) {
    this.isNewer = true;
    this.sinceTime = sinceTime;
    return this;
  }
  
  public AStreamQueryBuilder older(long sinceTime) {
    this.isNewer = false;
    this.sinceTime = sinceTime;
    return this;
  }
  
  public AStreamQueryBuilder equalType(StreamType type) {
    this.type = type;
    this.equalType = true;
    return this;
  }
  
  public AStreamQueryBuilder notEqualType(StreamType type) {
    this.type = type;
    this.equalType = false;
    return this;
  }
  
  public AStreamQueryBuilder ascOrder() {
    this.descOrder = false;
    return this;
  }
  
  public AStreamQueryBuilder descOrder() {
    this.descOrder = true;
    return this;
  }
  
  public TypedQuery<Activity> build() {
    EntityManager em = GenericDAOImpl.lifecycleLookup().getCurrentEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Activity> criteria = cb.createQuery(Activity.class);
    Root<Activity> activity = criteria.from(Activity.class);
    Join<Activity, StreamItem> streamItem = activity.join(Activity_.streamItems);
    
    List<Predicate> predicates = new ArrayList<Predicate>();
    //owner
    if (this.owner != null) {
      predicates.add(cb.equal(streamItem.get(StreamItem_.ownerId), owner.getId()));
    }
    
    //type
    if (this.type != null) {
      if (equalType) {
        predicates.add(cb.equal(streamItem.<StreamType>get(StreamItem_.streamType), this.type));
      } else {
        predicates.add(cb.notEqual(streamItem.<StreamType>get(StreamItem_.streamType), this.type));
      }
    }
    
    //newer or older
    if (this.sinceTime > 0) {
      if (isNewer) {
        predicates.add(cb.greaterThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
      } else {
        predicates.add(cb.lessThan(activity.<Long>get(Activity_.lastUpdated), this.sinceTime));
      }
    }

    predicates.add(cb.equal(activity.<Boolean>get(Activity_.hidden), Boolean.FALSE));
    predicates.add(cb.equal(activity.<Boolean>get(Activity_.locked), Boolean.FALSE));
    
    CriteriaQuery<Activity> select = criteria.select(activity);
    select.where(predicates.toArray(new Predicate[0]));
    if (this.descOrder) {
      select.orderBy(cb.desc(activity.<Long> get(Activity_.lastUpdated)));
    } else {
      select.orderBy(cb.asc(activity.<Long> get(Activity_.lastUpdated)));
    }

    TypedQuery<Activity> typedQuery = em.createQuery(select);
    if (this.limit > 0) {
      typedQuery.setFirstResult((int) offset);
      typedQuery.setMaxResults((int) limit);
    }
    
    return typedQuery;
  }
}
