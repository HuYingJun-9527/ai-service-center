package com.hyj.aicodehelper.controller;

import com.hyj.aicodehelper.tools.CityDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/city")
@Tag(name = "城市数据管理", description = "城市编码数据的CRUD操作API")
public class CityDataController {

    private final CityDataService cityDataService;

    public CityDataController(CityDataService cityDataService) {
        this.cityDataService = cityDataService;
    }

    /**
     * 解析并存储城市数据到Redis
     */
    @PostMapping("/parse-and-store")
    @Operation(summary = "解析并存储城市数据", description = "解析test.json文件并将城市数据存储到Redis中")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "存储成功"),
            @ApiResponse(responseCode = "500", description = "存储失败")
    })
    public String parseAndStore() {
        int count = cityDataService.parseAndStoreCityData();
        if (count > 0) {
            return "成功存储 " + count + " 条城市数据到Redis";
        } else {
            return "存储失败";
        }
    }

    /**
     * 根据城市名获取编码
     */
    @GetMapping("/code/{cityName}")
    @Operation(summary = "获取城市编码", description = "根据城市名称获取对应的城市编码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "城市不存在")
    })
    public String getCityCode(
            @Parameter(description = "城市名称", example = "北京", required = true)
            @PathVariable String cityName) {
        return cityDataService.getCityCode(cityName);
    }

    /**
     * 获取所有城市编码
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有城市编码", description = "获取所有城市名称和编码的映射关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Map<String, String> getAllCityCodes() {
        return cityDataService.getAllCityCodes();
    }

    /**
     * 更新城市编码
     */
    @PutMapping("/update")
    @Operation(summary = "更新城市编码", description = "更新指定城市的编码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public String updateCityCode(
            @Parameter(description = "城市名称", example = "北京", required = true)
            @RequestParam String cityName,
            @Parameter(description = "新的城市编码", example = "101010100", required = true)
            @RequestParam String newCode) {
        boolean success = cityDataService.updateCityCode(cityName, newCode);
        return success ? "更新成功" : "更新失败";
    }

    /**
     * 删除城市编码
     */
    @DeleteMapping("/delete/{cityName}")
    @Operation(summary = "删除城市编码", description = "删除指定城市的编码数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public String deleteCityCode(
            @Parameter(description = "城市名称", example = "北京", required = true)
            @PathVariable String cityName) {
        boolean success = cityDataService.deleteCityCode(cityName);
        return success ? "删除成功" : "删除失败";
    }
}