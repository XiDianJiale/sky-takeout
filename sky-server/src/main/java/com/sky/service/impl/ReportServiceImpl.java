package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService; //Service不仅仅可以调用Mapper,还可以调用其他Service

    /**     * 获取营业额统计报表
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 营业额统计报表VO
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //获取横坐标
        List<LocalDate> dateList = new ArrayList(); //存放begin到end之间的每天的日期

        if (end.isBefore(begin)) {
            throw new IllegalArgumentException("end 日期不能早于 begin 日期");
        }
        while (!begin.equals(end)) {

            dateList.add(begin);
            begin = begin.plusDays(1);

        }
        dateList.add(end);
        


        //获取纵坐标turnoverList
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);


            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            if (turnover == null) {
                turnover = 0.0;
            }
            turnoverList.add(turnover);

        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }


    /**     * 获取用户统计报表
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 用户统计报表VO
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        //获取横坐标
        List<LocalDate> dateList = new ArrayList(); //存放begin到end之间的每天的日期

        if (end.isBefore(begin)) {
            throw new IllegalArgumentException("end 日期不能早于 begin 日期");
        }
        while (!begin.equals(end)) {

            dateList.add(begin);
            begin = begin.plusDays(1);

        }
        dateList.add(end);

        //获取纵坐标（一个新增用户数量，一个总用户数量）
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        //sql: select count(id) from user where create_time between ? and ?


        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);

            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }


        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }



    /**     * 获取订单统计报表
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 订单统计报表VO
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        //获取横坐标
        List<LocalDate> dateList = new ArrayList(); //存放begin到end之间的每天的日期

        if (end.isBefore(begin)) {
            throw new IllegalArgumentException("end 日期不能早于 begin 日期");
        }
        while (!begin.equals(end)) {

            dateList.add(begin);
            begin = begin.plusDays(1);

        }
        dateList.add(end);

        //获取纵坐标,5个:每日订单数，每日有效订单数，
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderList = new ArrayList<>();


        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            Integer orderCount = getOrderCount(beginTime, endTime, null); //copilot补全把begin和end写反了,导致第一次调试的时候totalOrder是0.0

            validOrderList.add(validOrderCount);
            orderCountList.add(orderCount);


        }


        //时间段内的总订单数遍历累加（就不查表了）
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer totalValidOrder = validOrderList.stream().reduce(Integer::sum).get();

        Double OrderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            OrderCompletionRate = totalValidOrder.doubleValue() / totalOrderCount;
        }


        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrder)
                .orderCompletionRate(OrderCompletionRate)
                .build();
    }



    /**     * 获取指定时间段内(或加上制定状态)的订单数量
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param status    订单状态（可选）
     * @return 订单数量
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
//        if (status != null) {     ai乱写的,这段代码完全没有用
//            map.put("status", status);
//        }
        return orderMapper.countByMap(map);
    }


    /**     * 获取销售排行榜前十报表
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 销售排行榜前十报表VO
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);


        List<GoodsSalesDTO> top10 = orderMapper.getTop10(beginTime, endTime);

        List<String> names = top10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = top10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }


    /**     * 导出数据报表
     *
     * @param response HttpServletResponse
     */
    public void export(HttpServletResponse response) {
        // 1.准备数据：mapper查  2.写数据到Excel中  3.通过response输出到浏览器下载
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                    LocalDateTime.of(LocalDate.now().minusDays(30), LocalTime.MIN),
                    LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX));


        //2.
        //基于模版创建一个excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取文件sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据：时间 营业额 。。。。
            sheet.getRow(1).getCell(1).setCellValue("时间：" + LocalDate.now().minusDays(30) + "至" + LocalDate.now().minusDays(1));
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            for (int i = 0; i < 30; i++) {  //不建议硬编码30不方便改报表导出需求
                LocalDate date = LocalDate.now().minusDays(30).plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }


            //3.
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            out.close();
            excel.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
        

    }
}
