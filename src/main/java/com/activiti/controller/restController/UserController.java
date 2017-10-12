package com.activiti.controller.restController;

import com.activiti.common.aop.ApiAnnotation;
import com.activiti.common.utils.CommonUtil;
import com.activiti.common.utils.ConstantsUtils;
import com.activiti.mapper.UserMapper;
import com.activiti.pojo.user.StudentWorkInfo;
import com.activiti.pojo.user.User;
import com.activiti.pojo.user.UserRole;
import com.activiti.service.JudgementService;
import com.activiti.service.ScheduleService;
import com.activiti.service.UserService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by 12490 on 2017/8/1.
 */
@RequestMapping("/api/user")
@RestController
public class UserController {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private CommonUtil commonUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private JudgementService judgementService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private UserMapper userMapper;

    /*
     *  根据Email获取用户信息
     */

    @RequestMapping("/getUserInfo")
    @ResponseBody
    @ApiAnnotation
    public Object getUserInfo(@RequestParam(value = "email", required = true) String email) throws Exception {
        return userService.findUserInfo(email);
    }

    /**
     * 提交作业
     *
     * @param courseCode
     * @param workDetail
     * @param request
     * @return
     */
    @RequestMapping("/commitWork")
    @ResponseBody
    @ApiAnnotation
    public Object commitWork(@RequestParam(name = "courseCode") String courseCode,
                             @RequestParam(name = "workDetail") String workDetail, HttpServletRequest request) throws Exception {
        String email = request.getSession().getAttribute(ConstantsUtils.sessionEmail).toString();
        Date deadline = scheduleService.selectScheduleTime(courseCode).getJudgeStartTime();
        if (commonUtil.compareDate(new Date(), deadline))
            throw new Exception("提交作业截至时间:" + CommonUtil.dateToString(deadline));
        StudentWorkInfo studentWorkInfo = new StudentWorkInfo(courseCode, email, workDetail, new Date());
        User user = new User(commonUtil.getRandomUserName(), studentWorkInfo.getEmailAddress());
        userService.insertUser(user);
        studentWorkInfo.setLastCommitTime(new Date());
        userService.insertUserWork(studentWorkInfo);
        return studentWorkInfo;
    }

    /**
     * 查询学生提交的作业
     *
     * @param courseCode
     * @return
     */
    @RequestMapping("/selectStudentWorkInfo")
    @ResponseBody
    @ApiAnnotation
    public Object selectStudentWorkInfo(
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "limit", required = false, defaultValue = "1") int limit,
            @RequestParam(value = "count", required = false) boolean count,
            HttpServletRequest request) {
        String email = request.getSession().getAttribute(ConstantsUtils.sessionEmail).toString();
        if (null != courseCode && !"".equals(courseCode)) {
            StudentWorkInfo studentWorkInfo = new StudentWorkInfo();
            studentWorkInfo.setCourseCode(courseCode);
            studentWorkInfo.setEmailAddress(email);
            return userService.selectStudentWorkInfo(studentWorkInfo);
        } else if (count) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("count", userMapper.countStudentWorkInfo(email));
            return jsonObject;
        } else {
            return userMapper.selectStudentWorkInfoPage(email, (page - 1) * limit, limit);
        }
    }

    @RequestMapping("/selectWorkListToJudge")
    @ResponseBody
    @ApiAnnotation
    public Object selectWorkListToJudge(@RequestParam(value = "email", required = true) String email,
                                        @RequestParam(value = "courseCode", required = true) String courseCode) {
        StudentWorkInfo studentWorkInfo = new StudentWorkInfo();
        studentWorkInfo.setCourseCode(courseCode);
        int studentId = judgementService.selectChaosId(email);
        int countWork = judgementService.countAllWorks(courseCode);
        int judgeTimes = scheduleService.selectScheduleTime(courseCode).getJudgeTimes();
        int[] initIdList = {studentId + 1, studentId + 2, studentId + 3};
        List<StudentWorkInfo> workInfoList = new ArrayList<>();
//        for (int id:initIdList){
//            if (id>countWork)id=id-countWork;
//            studentWorkInfo.setEmailAddress(userService);
//            workInfoList.add(userService.selectStudentWorkInfo())
//        }
        return null;
    }

    /**
     * 删除管理员用户
     *
     * @param email
     * @return
     */
    @ResponseBody
    @RequestMapping("/deleteUserRole")
    @ApiAnnotation
    public Object deleteUserRole(@RequestParam(value = "email", required = true) String email) {
        return userService.deleteUserRole(email);
    }

    /**
     * 添加管理员用户
     *
     * @param email
     * @param id
     * @param remarks
     * @return
     */
    @ResponseBody
    @RequestMapping("/addUserRole")
    @ApiAnnotation
    public Object addUserRole(@RequestParam(value = "email", required = true) String email,
                              @RequestParam(value = "id", required = true) int id,
                              @RequestParam(value = "remarks", required = true) String remarks) throws Exception {
        if (!commonUtil.emailFormat(email))
            throw new Exception("邮箱格式不正确");
        return userService.insertUserRole(new UserRole(id, email, remarks));
    }
}

