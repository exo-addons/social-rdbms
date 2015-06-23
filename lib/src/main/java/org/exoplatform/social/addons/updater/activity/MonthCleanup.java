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
package org.exoplatform.social.addons.updater.activity;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.updater.ActivityMigrationService;
import org.exoplatform.social.common.service.utils.TraceElement;
import org.exoplatform.social.core.chromattic.entity.ActivityListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityMonthEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityRefListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityRefMonthEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityRefYearEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityYearEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.storage.impl.AbstractStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          tuvd@exoplatform.com
 * Jun 22, 2015  
 */
public class MonthCleanup {
  /**
   * Cleanup the activity from MonthNode
   */
  public static AbstractStrategy<IdentityEntity> activityMonthRefCleanup = new AbstractStrategy<IdentityEntity>() {
    private final Log LOG = ExoLogger.getLogger("ActivityRefMonthCleanupStrategy");
    private final String PROCESS_NAME = "ACTIVITY_REF_MONTH_CLEANUP_PROCESSING";
    public boolean process(IdentityEntity entity) throws RuntimeException {

      TraceElement trace = tracer.addElement(ActivityMigrationService.EVENT_LISTENER_KEY, PROCESS_NAME);
      AtomicInteger size = new AtomicInteger(0);
      try {
        trace.start();
        //feed
        RefMonthIterator dayIter = new RefMonthIterator(entity.getStreams().getAll());
        size.addAndGet(removeElement(dayIter));
        //connections
        dayIter = new RefMonthIterator(entity.getStreams().getConnections());
        size.addAndGet(removeElement(dayIter));
        //my spaces
        dayIter = new RefMonthIterator(entity.getStreams().getMySpaces());
        size.addAndGet(removeElement(dayIter));

        //my activities
        dayIter = new RefMonthIterator(entity.getStreams().getOwner());
        size.addAndGet(removeElement(dayIter));

        //my activities
        dayIter = new RefMonthIterator(entity.getStreams().getSpace());
        size.addAndGet(removeElement(dayIter));

        trace.end();
        return true;
      } catch (RuntimeException e) {
        LOG.error(e.getMessage(), e);
        return false;
      } finally {
        LOG.info(trace.toString());
        LOG.info(PROCESS_NAME + "::removed size:: " + size.intValue());
      }
    };
  };

  /**
   * Cleanup the activity from MonthNode
   */
  public static AbstractStrategy<IdentityEntity> activityMonthCleanup = new AbstractStrategy<IdentityEntity>() {
    private final Log LOG = ExoLogger.getLogger("ActivityMonthCleanupStrategy");
    private final String PROCESS_NAME = "ACTIVITY_MONTH_CLEANUP_PROCESSING";
    @Override
    public boolean process(IdentityEntity entity) throws RuntimeException {
      TraceElement trace = tracer.addElement(ActivityMigrationService.EVENT_LISTENER_KEY, PROCESS_NAME);
      int size = 0;
      try {
        trace.start();
        MonthIterator dayIter = new MonthIterator(entity.getActivityList());
        size = removeElement(dayIter);
        trace.end();
        return true;
      } catch (RuntimeException e) {
        return false;
      } finally {
        LOG.info(trace.toString());
        LOG.info(PROCESS_NAME + "::removed size:: " + size);
      }
    };
  };

  /**
   * The RefMonthIterator over a ActivityRefMonth collection.
   * The underlying resources implemented by org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList
   * The invocation removes the element will be invoked the method in org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList#remove
   * 
   * @author tuvd
   *
   */
  public static class RefMonthIterator implements Iterator<ActivityRefMonthEntity> {

    private Iterator<ActivityRefYearEntity> yearIterator;
    private Iterator<ActivityRefMonthEntity> monthIterator;
    
    RefMonthIterator(final ActivityRefListEntity listEntity) {
      this.yearIterator = listEntity.getYears().values().iterator();
      if (yearIterator.hasNext()) {
        this.monthIterator = yearIterator.next().getMonthsList().iterator();
      }
    }

    @Override
    public boolean hasNext() {
      boolean nothing = true;
      if (monthIterator != null && monthIterator.hasNext()) {
        return true;
      } else if (yearIterator != null && yearIterator.hasNext()) {
        monthIterator = yearIterator.next().getMonthsList().iterator();
        nothing = false;
        if (monthIterator.hasNext()) {
          return true;
        }
      }
      if (nothing) {
        return false;
      } else {
        return hasNext();
      }
    }

    @Override
    public ActivityRefMonthEntity next() {
      if (hasNext()) {
        return monthIterator.next();
      } else {
        throw new RuntimeException();
      }
    }

    @Override
    public void remove() {
      if (hasNext()) {
        AbstractStorage.lifecycleLookup().getSession().remove(this.next());
      }
    }
  }

  /**
   * The MonthIterator over a ActivityMonth collection.
   * The underlying implemented by org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList
   * The invocation removes the element will be invoked the method in org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList#remove
   * 
   * @author thanhvc
   *
   */
  public static class MonthIterator implements Iterator<ActivityMonthEntity> {

    private Iterator<ActivityYearEntity> yearIterator;
    private Iterator<ActivityMonthEntity> monthIterator;
    
    MonthIterator(final ActivityListEntity listEntity) {
      this.yearIterator = listEntity.getYears().values().iterator();
      if (yearIterator.hasNext()) {
        this.monthIterator = yearIterator.next().getMonthsList().iterator();
      }
    }

    @Override
    public boolean hasNext() {
      boolean nothing = true;
      if (monthIterator != null && monthIterator.hasNext()) {
        return true;
      } else if (yearIterator != null && yearIterator.hasNext()) {
        monthIterator = yearIterator.next().getMonthsList().iterator();
        nothing = false;
        if (monthIterator.hasNext()) {
          return true;
        }
      }
      if (nothing) {
        return false;
      } else {
        return hasNext();
      }
    }

    @Override
    public ActivityMonthEntity next() {
      if (hasNext()) {
        return monthIterator.next();
      } else {
        throw new RuntimeException();
      }
    }

    @Override
    public void remove() {
      if (hasNext()) {
        AbstractStorage.lifecycleLookup().getSession().remove(this.next());
      }
    }
  }
}