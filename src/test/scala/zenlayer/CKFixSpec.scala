/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package me.ooon.workia
package zenlayer

import com.typesafe.scalalogging.StrictLogging
import test.BaseSpec
import com.github.nscala_time.time.Imports._

/**
 * CKFixSpec
 *
 * @author zhaihao
 * @version 1.0
 * @since 2022/7/21 18:41
 */
class CKFixSpec extends BaseSpec with StrictLogging {

  // clickhouse-client --port 19000 -u admin --password *** -m -n < refresh_flow.sql

  /**
   * select extractAllGroups(query,'.*WITH.*(\d{4}-\d{2}-\d{2}).*(\d{4}-\d{2}-\d{2}).*') as range,
   *       type,event_time,exception_code,query_duration_ms,read_rows,written_rows,query,databases,exception
   * from system.query_log
   * where user='admin' and event_time>='2022-07-21 10:55:00' and databases=['aiop','aiop_local']
   * order by event_time_microseconds desc;
   */
  "批量刷新flow表" in {
    var start = LocalDateTime.parse("2022-01-01T00:00:00")
    val end  = LocalDateTime.parse("2022-05-01T00:00:00")
    val step = 1.days

    val file = os.home / "refresh_flow.sql"
    if (os.exists(file)) os.remove(file)

    while (start < end) {
      val tmp = start + step
      val sql =
        s"""
           |INSERT INTO aiop.flow (flow_id, flow_type, rate_in, rate_out, timestamp, create_time)
           |SELECT
           |    flow_id AS flow_id,
           |    flow_type AS flow_type,
           |    sum(in) AS rate_in,
           |    sum(out) AS rate_out,
           |    time_particle AS timestamp,
           |    now() AS create_time
           |FROM
           |(
           |    SELECT
           |        avg(byte_rate_in) * 8 AS in,
           |        avg(byte_rate_out) * 8 AS out,
           |        ts2 AS time_particle,
           |        flow_id,
           |        flow_type AS flow_type,
           |        any(start_time) AS start_time,
           |        any(end_time) AS end_time
           |    FROM
           |    (
           |        SELECT
           |            if((lag_timestamp = 0) OR ((timestamp - lag_timestamp) > (2 * group_timegrand)) OR ((byte_in - lag_byte_in) < 0), 0, (byte_in - lag_byte_in) / (timestamp - lag_timestamp)) AS tmp_byte_rate_in,
           |            if(tmp_byte_rate_in >= (1000000000000. / 8), 0, tmp_byte_rate_in) AS byte_rate_in,
           |            if((lag_timestamp = 0) OR ((timestamp - lag_timestamp) > (2 * group_timegrand)) OR ((byte_out - lag_byte_out) < 0), 0, (byte_out - lag_byte_out) / (timestamp - lag_timestamp)) AS tmp_byte_rate_out,
           |            if(tmp_byte_rate_out >= (1000000000000. / 8), 0, tmp_byte_rate_out) AS byte_rate_out,
           |            snmp_id,
           |            index,
           |            flow_id,
           |            flow_type,
           |            start_time,
           |            end_time,
           |            ts2
           |        FROM
           |        (
           |            SELECT
           |                byte_in,
           |                lagInFrame(byte_in) OVER (PARTITION BY flow_id, flow_type, snmp_id, index ORDER BY ts1 ASC) AS lag_byte_in,
           |                byte_out,
           |                lagInFrame(byte_out) OVER (PARTITION BY flow_id, flow_type, snmp_id, index ORDER BY ts1 ASC) AS lag_byte_out,
           |                timestamp,
           |                lagInFrame(timestamp) OVER (PARTITION BY flow_id, flow_type, snmp_id, index ORDER BY ts1 ASC) AS lag_timestamp,
           |                snmp_id,
           |                index,
           |                flow_id,
           |                flow_type,
           |                start_time,
           |                end_time,
           |                toUInt64(ts1 / timegrand) * timegrand AS ts2,
           |                group_timegrand
           |            FROM
           |            (
           |                SELECT
           |                    any(in) AS byte_in,
           |                    any(out) AS byte_out,
           |                    any(first_timestamp) AS timestamp,
           |                    any(timegrand) AS timegrand,
           |                    any(group_timegrand) AS group_timegrand,
           |                    snmp_id,
           |                    index,
           |                    flow_id,
           |                    flow_type AS flow_type,
           |                    any(start_time) AS start_time,
           |                    any(end_time) AS end_time,
           |                    ts1
           |                FROM
           |                (
           |                    WITH (300 AS out_timegrand, 300 AS group_timegrand, toUnixTimestamp('${start.toString(
          "yyyy-MM-dd HH:mm:ss"
        )}') AS start_time, toUnixTimestamp('${tmp.toString("yyyy-MM-dd HH:mm:ss")}') AS end_time)
           |                    SELECT
           |                        snmp_id,
           |                        index,
           |                        flow_id,
           |                        flow_type,
           |                        first_value(timestamp) OVER (PARTITION BY flow_id, flow_type, snmp_id, index, toUInt64(timestamp / group_timegrand) ORDER BY timestamp ASC) AS first_timestamp,
           |                        first_value(in) OVER (PARTITION BY flow_id, flow_type, snmp_id, index, toUInt64(timestamp / group_timegrand) ORDER BY timestamp ASC) AS in,
           |                        first_value(out) OVER (PARTITION BY flow_id, flow_type, snmp_id, index, toUInt64(timestamp / group_timegrand) ORDER BY timestamp ASC) AS out,
           |                        out_timegrand AS timegrand,
           |                        group_timegrand,
           |                        start_time,
           |                        end_time,
           |                        toUInt64(timestamp / group_timegrand) * group_timegrand AS ts1
           |                    FROM aiop.port_flow2 AS tl
           |                    WHERE (flow_id != 0) AND (timestamp >= (start_time - group_timegrand)) AND (timestamp < (end_time + group_timegrand))
           |                ) AS t1
           |                GROUP BY
           |                    flow_id,
           |                    flow_type,
           |                    snmp_id,
           |                    index,
           |                    ts1
           |            ) AS t2
           |        ) AS t3
           |    ) AS t4
           |    GROUP BY
           |        flow_id,
           |        flow_type,
           |        snmp_id,
           |        index,
           |        ts2
           |) AS t2
           |WHERE (time_particle >= start_time) AND (time_particle < end_time)
           |GROUP BY
           |    flow_id,
           |    flow_type,
           |    time_particle;
           |""".stripMargin

      os.write.append(file, sql)
      start = tmp
    }
  }
}
