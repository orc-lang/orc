//
// PlacementController.scala -- Scala trait PlacementController
// Project OrcScala
//
// Created by jthywiss on Feb 10, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.common

import java.util.ServiceLoader

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

import orc.Future
import orc.run.distrib.{ AbstractLocation, CallLocationOverrider, ClusterLocations, DOrcPlacementPolicy, ValueLocator, ValueLocatorFactory }

/** The manager for value locations that is mixed into the DOrcExecution.
  *
  * @author jthywiss
  */
trait PlacementController[Location <: AbstractLocation] {
  rrim: RemoteRefIdManager[Location] =>

  val runtime: ClusterLocations[Location]

  protected val valueLocatorFactoryServiceLoader: ServiceLoader[ValueLocatorFactory[Location]] = ServiceLoader.load(classOf[ValueLocatorFactory[Location]])

  protected val valueLocators: Set[ValueLocator[Location]] = valueLocatorFactoryServiceLoader.asScala.map(_(runtime)).toSet

  protected val callLocationOverriders: Set[CallLocationOverrider[Location]] = valueLocators.filter(_.isInstanceOf[CallLocationOverrider[Location]]).asInstanceOf[Set[CallLocationOverrider[Location]]]

  def currentLocations(v: Any): Set[Location] = {
    def pfc(v: Any): PartialFunction[ValueLocator[Location], Set[Location]] =
      { case vl if vl.currentLocations.isDefinedAt(v) => vl.currentLocations(v) }
    val cl = v match {
      case ro: RemoteObjectRef => Set(rrim.homeLocationForRemoteRef(ro.remoteRefId))
      case rf: Future with RemoteRef => Set(rrim.homeLocationForRemoteRef(rf.remoteRefId), runtime.here)
      case _ if valueLocators.exists(_.currentLocations.isDefinedAt(v)) => valueLocators.collect(pfc(v)).reduce(_.union(_))
      case _ => runtime.hereSet
    }
    //Logger.ValueLocation.finer(s"currentLocations($v: ${orc.util.GetScalaTypeName(v)})=$cl")
    cl
  }

  def isLocal(v: Any): Boolean = {
    val result = v match {
      case ro: RemoteObjectRef => false
      case rf: Future with RemoteRef => true
      case _ if valueLocators.exists(_.currentLocations.isDefinedAt(v)) => valueLocators.exists({ vl => vl.currentLocations.isDefinedAt(v) && vl.valueIsLocal(v) })
      case _ => true
    }
    //Logger.ValueLocation.finer(s"isLocal($v: ${orc.util.GetScalaTypeName(v)})=$result")
    result
  }

  def permittedLocations(v: Any): Set[Location] = {
    def pfp(v: Any): PartialFunction[ValueLocator[Location], Set[Location]] =
      { case vl if vl.permittedLocations.isDefinedAt(v) => vl.permittedLocations(v) }
    val pl = v match {
      case plp: DOrcPlacementPolicy => plp.permittedLocations(runtime)
      case ro: RemoteObjectRef => Set(rrim.homeLocationForRemoteRef(ro.remoteRefId))
      case rf: Future with RemoteRef => runtime.allLocations
      case _ if valueLocators.exists(_.permittedLocations.isDefinedAt(v)) => valueLocators.collect(pfp(v)).reduce(_.intersect(_))
      case _ => runtime.allLocations
    }
    //Logger.ValueLocation.finest(s"permittedLocations($v: ${orc.util.GetScalaTypeName(v)})=$pl")
    pl
  }

  def callLocationMayNeedOverride(target: AnyRef, arguments: Array[AnyRef]): Option[Boolean] = {
    //val cloResults = callLocationOverriders.map(_.callLocationMayNeedOverride(target, arguments)).filter(_.isDefined).map(_.get)
    //assert(cloResults.size <= 1, "Multiple CallLocationOverriders responded for " + orc.util.GetScalaTypeName(target) + arguments.map(orc.util.GetScalaTypeName(_)).mkString("(", ",", ")"))
    //cloResults.headOption
    for (clo <- callLocationOverriders) {
      val needsOverride = clo.callLocationMayNeedOverride(target, arguments)
      /* Short-circuit: First clo to answer wins.  Masks multiple clo bugs, though. */
      if (needsOverride.isDefined) return needsOverride
    }
    None
  }

  def callLocationOverride(target: AnyRef, arguments: Array[AnyRef]): Set[Location] = {
    for (clo <- callLocationOverriders) {
      val needsOverride = clo.callLocationMayNeedOverride(target, arguments)
      /* Short-circuit: First clo to answer wins.  Masks multiple clo bugs, though. */
      if (needsOverride.isDefined) return clo.callLocationOverride(target, arguments)
    }
    Set.empty
  }

}
