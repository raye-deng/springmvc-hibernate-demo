package cm.controller;

import cm.entity.CacheDataChangeLog;
import cm.entity.ServerConcurrentLog;
import cm.entity.ServerInfo;
import cm.service.CacheLogService;
import cm.service.ConcurrentLogService;
import cm.service.RedisService;
import cm.service.TimeOutLogService;
import cm.web.MediaTypes;
import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.Timestamp;
import java.util.*;


@Controller
@RequestMapping("/analysis/non-bussiness")
public class NonBusinessController extends BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(NonBusinessController.class);

    @Autowired
    private ConcurrentLogService concurrentLogService;

    @Autowired
    private TimeOutLogService timeOutLogService;

    @Autowired
    private CacheLogService cacheLogService;
    @Autowired
    private RedisService redisService;

    /**
     * 服务器节点并发节点数据
     *
     * @param modelMap
     * @param time
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/concurrent/list", method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    public String concurrentList(ModelMap modelMap, @RequestParam(value = "time") Long time) {
        try {
//            redisService.addTestData();
            List<ServerInfo> servers = concurrentLogService.findAllServer();
            List<String> serverNames = new ArrayList();
            for (ServerInfo info : servers) {
                serverNames.add(info.getServerName());
            }
            List logData = new ArrayList();
            List<String> xAxis = new ArrayList<>();
            Timestamp timestamp = new Timestamp(time);
            for (int i = 0; i < 24; i++) {
                xAxis.add(i + ":00");
            }
            int count = 0;
            for (ServerInfo server : servers) {

                List<ServerConcurrentLog> logs = concurrentLogService.findConcurrentByCondition(server, timestamp);
                Map logMap = new HashMap();
                logMap.put("name", server.getServerName());
                List<Long> amounts = new ArrayList<>();

                for (ServerConcurrentLog log : logs) {
                    amounts.add(log.getConcurrentAmount());
                    count += log.getConcurrentAmount();
                }
                logMap.put("data", amounts);

                logMap.put("type", "line");
                logMap.put("stack", "总量");
                logData.add(logMap);
            }
            if (count <= 0) {
                throw new RuntimeException("没有找到当前时间的日志数据,服务器可能在维护!");
            }
            Map data = new HashMap();
            data.put("series", logData);
            data.put("xAxis", xAxis);
            data.put("legend", serverNames);
            modelMap.put("data", data);
            this.success(modelMap);
        } catch (RuntimeException ex) {
            this.failed(modelMap, ex.getMessage());
            LOGGER.error("get concurrent log list occur error:", ex);
        } catch (Exception ex) {
            this.failed(modelMap);
            LOGGER.error("get concurrent log list occur error:", ex);
        }

        return JSON.toJSONString(modelMap);
    }


    /**
     * 获取服务器节点数据
     *
     * @param modelMap
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/server/name/list", method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    public String serverList(ModelMap modelMap) {
        try {
            List<ServerInfo> serverInfos = concurrentLogService.findAllServer();
            List<String> serverNames = new ArrayList();
            for (ServerInfo info : serverInfos) {
                serverNames.add(info.getServerName());
            }
            modelMap.put("data", serverNames);
            this.success(modelMap);
        } catch (Exception ex) {
            this.failed(modelMap);
            LOGGER.error("get concurrent log list occur error:", ex);
        }

        return JSON.toJSONString(modelMap);
    }


    /**
     * 获取某天的所有服务器节点所有接口的超时请求统计
     *
     * @param modelMap
     * @param logTime  日志纪录时间 精确到天
     * @return
     */
    @ResponseBody
    @RequestMapping("/timeout/list")
    public String timeoutList(ModelMap modelMap, @RequestParam(value = "logTime") Long logTime) {
        try {
            Map<String, List> serversDateCount = timeOutLogService.findAllServerTimeoutLogByDate(new Timestamp(logTime));
            Map<String, Object> data = new HashMap<>();
            data.put("xAxis", getXAxis());
            data.put("series", serversDateCount);
            modelMap.put("data", data);
            this.success(modelMap);
        } catch (RuntimeException ex) {
            this.failed(modelMap, ex.getMessage());
            LOGGER.error("server time out list occur error:", ex);
        } catch (Exception ex) {
            this.failed(modelMap);
            LOGGER.error("server time out list occur error:", ex);
        }

        return JSON.toJSONString(modelMap);
    }

    @ResponseBody
    @RequestMapping(value = "/cache/list")
    public String cacheList(ModelMap modelMap, @RequestParam(value = "logTime") Long logTime) {
        try {
            List<CacheDataChangeLog> logs = cacheLogService.findByLogTime(new Timestamp(logTime));
            if (CollectionUtils.isEmpty(logs)) {
                throw new RuntimeException("未找到对应的日志数据，服务器可能处于维护状态!");
            }
            Map data = new HashMap();
            Map series = new HashMap();
            List<Long> sData = new ArrayList<>();
            for (CacheDataChangeLog log : logs) {
                sData.add(log.getDataAmount());
            }
            series.put("data", sData);
            series.put("name", "redis缓存数据变化统计");
            series.put("type", "line");
            data.put("series", series);
            data.put("xAxis", getXAxis());
            modelMap.put("data", data);
            this.success(modelMap);
        } catch (RuntimeException ex) {
            this.failed(modelMap, ex.getMessage());
            LOGGER.error("get cache data amount log list occur error:", ex);
        } catch (Exception ex) {
            this.failed(modelMap);
            LOGGER.error("get cache data amount log list occur error:", ex);
        }
        return JSON.toJSONString(modelMap);
    }

}
