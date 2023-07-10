/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

import sbt.Keys._
import sbt._
import sbt.plugins._

import scala.Console.{RESET, YELLOW}

/**
 * EnvPlugin
 * 运行时修改  set env := Env.dev
 *
 * @author zhaihao
 * @version 1.0
 * @since 2022/1/16 11:37
 */
object EnvPlugin extends AutoPlugin {

  override def trigger = noTrigger

  override def requires = JvmPlugin

  object autoImport {

    object Env extends Enumeration {
      val prod, stage, test, dev = Value
    }

    val env = settingKey[Env.Value]("the current build environment")
  }

  import autoImport._

  //noinspection DuplicatedCode
  override def projectSettings: Seq[Setting[_]] = Seq(
    env := {
      sys.props
        .get("env")
        .orElse(sys.env.get("BUILD_ENV"))
        .flatMap(e => Some(Env.withName(e.toLowerCase)))
        .getOrElse(Env.dev)
    },
    // give a feed back
    onLoadMessage := {
      // append on the old message as well
      val defaultMessage = onLoadMessage.value

      s"""|$defaultMessage
          |Running in environment profile: [ $YELLOW${env.value}$RESET ]""".stripMargin
    },
    // 因为play有自己的定义，需要在 build.sbt 中进行覆盖
    Compile / unmanagedResourceDirectories += (Compile / resourceDirectory).value.getParentFile / "common",
    Compile / resourceDirectory := (Compile / resourceDirectory).value / env.value.toString
  )
}
