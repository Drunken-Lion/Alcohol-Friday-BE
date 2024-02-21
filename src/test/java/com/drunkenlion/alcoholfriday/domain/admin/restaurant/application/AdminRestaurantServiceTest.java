package com.drunkenlion.alcoholfriday.domain.admin.restaurant.application;

import com.drunkenlion.alcoholfriday.domain.admin.restaurant.dto.RestaurantDetailResponse;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.dto.RestaurantListResponse;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.dto.RestaurantRequest;
import com.drunkenlion.alcoholfriday.domain.auth.enumerated.ProviderType;
import com.drunkenlion.alcoholfriday.domain.item.entity.Item;
import com.drunkenlion.alcoholfriday.domain.member.dao.MemberRepository;
import com.drunkenlion.alcoholfriday.domain.member.entity.Member;
import com.drunkenlion.alcoholfriday.domain.member.enumerated.MemberRole;
import com.drunkenlion.alcoholfriday.domain.restaurant.dao.RestaurantRepository;
import com.drunkenlion.alcoholfriday.domain.restaurant.dao.RestaurantStockRepository;
import com.drunkenlion.alcoholfriday.domain.restaurant.entity.Restaurant;
import com.drunkenlion.alcoholfriday.domain.restaurant.entity.RestaurantStock;
import com.drunkenlion.alcoholfriday.domain.restaurant.enumerated.DayInfo;
import com.drunkenlion.alcoholfriday.domain.restaurant.enumerated.Provision;
import com.drunkenlion.alcoholfriday.domain.restaurant.enumerated.TimeOption;
import com.drunkenlion.alcoholfriday.domain.restaurant.vo.TimeData;
import com.drunkenlion.alcoholfriday.global.common.response.HttpResponse;
import com.drunkenlion.alcoholfriday.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Transactional
public class AdminRestaurantServiceTest {
    @InjectMocks
    private AdminRestaurantServiceImpl adminRestaurantService;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private RestaurantStockRepository restaurantStockRepository;
    @Mock
    private MemberRepository memberRepository;
    final GeometryFactory geometryFactory = new GeometryFactory();

    private final Long memberId = 1L;
    private final String email = "test@example.com";
    private final String provider = ProviderType.KAKAO.getProviderName();
    private final String memberName = "테스트";
    private final String nickname = "test";
    private final String role = MemberRole.MEMBER.getRole();
    private final Long phone = 1012345678L;
    private final LocalDate certifyAt = LocalDate.now();
    private final boolean agreedToServiceUse = true;
    private final boolean agreedToServicePolicy = true;
    private final boolean agreedToServicePolicyUse = true;
    private final LocalDateTime memberCreatedAt = LocalDateTime.now();

    private final Long id = 1L;
    private final String category = "한식";
    private final String name = "맛있는 한식당";
    private final String address = "서울시 강남구";
    final Coordinate coordinate1 = new Coordinate(127.0276, 37.4979);
    private final Point location = geometryFactory.createPoint(coordinate1);
    private final Long contact = 1012345678L;

    private Map<String, Object> getMenuTest() {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("비빔밥", 8000);
        frame.put("불고기", 12000);
        return frame;
    }

    private Map<String, Object> getTimeTest() {
        Map<String, Object> allDayTime = new LinkedHashMap<>();

        allDayTime.put(TimeOption.HOLIDAY.toString(), true);
        allDayTime.put(TimeOption.ETC.toString(), "명절 당일만 휴업");

        TimeData timeData = TimeData.builder()
                .businessStatus(true)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .breakBusinessStatus(true)
                .breakStartTime(LocalTime.of(15, 0))
                .breakEndTime(LocalTime.of(17, 0))
                .build();

        for (DayInfo value : DayInfo.values()) {
            allDayTime.put(value.toString(), timeData);
        }

        return allDayTime;
    }

    private Map<String, Object> getProvisionTest() {
        Map<String, Object> frame = new LinkedHashMap<>();

        for (Provision value : Provision.values()) {
            frame.put(value.toString(), true);
        }
        return frame;
    }

    private final Map<String, Object> menu = getMenuTest();
    private final Map<String, Object> time = getTimeTest();
    private final Map<String, Object> provision = getProvisionTest();
    private final LocalDateTime createdAt = LocalDateTime.now();
    private final int page = 0;
    private final int size = 20;

    private final Long modifyMemberId = 2L;
    private final String modifyNickname = "test 수정";
    private final String modifyCategory = "한식 수정";
    private final String modifyName = "맛있는 한식당 수정";
    private final String modifyAddress = "서울시 강남구 수정";
    final Coordinate coordinate2 = new Coordinate(20.0002, 10.0001);
    private final Point modifyLocation = geometryFactory.createPoint(coordinate2);
    private final Long modifyContact = 1011112222L;

    private Map<String, Object> getModifyMenuTest() {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("비빔밥 수정", 8000);
        frame.put("불고기 수정", 12000);
        frame.put("닭갈비 수정", 20000);
        return frame;
    }

    private Map<String, Object> getModifyTimeTest() {
        Map<String, Object> allDayTime = new LinkedHashMap<>();

        allDayTime.put(TimeOption.HOLIDAY.toString(), true);
        allDayTime.put(TimeOption.ETC.toString(), "명절 당일만 휴업 수정");

        TimeData timeData = TimeData.builder()
                .businessStatus(true)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .breakBusinessStatus(true)
                .breakStartTime(LocalTime.of(15, 0))
                .breakEndTime(LocalTime.of(17, 0))
                .build();

        for (DayInfo value : DayInfo.values()) {
            allDayTime.put(value.toString(), timeData);
        }

        return allDayTime;
    }

    private Map<String, Object> getModifyProvisionTest() {
        Map<String, Object> frame = new LinkedHashMap<>();

        for (Provision value : Provision.values()) {
            frame.put(value.toString(), false);
        }
        return frame;
    }

    private final Map<String, Object> modifyMenu = getModifyMenuTest();
    private final Map<String, Object> modifyTime = getModifyTimeTest();
    private final Map<String, Object> modifyProvision = getModifyProvisionTest();

    @Test
    @DisplayName("매장 목록 조회 성공")
    public void getRestaurantsTest() {
        // given
        when(this.restaurantRepository.findAll(any(Pageable.class))).thenReturn(this.getRestaurants());

        // when
        Page<RestaurantListResponse> restaurants = this.adminRestaurantService.getRestaurants(page, size);

        // then
        List<RestaurantListResponse> content = restaurants.getContent();

        assertThat(content).isInstanceOf(List.class);
        assertThat(content.size()).isEqualTo(1);
        assertThat(content.get(0).getId()).isEqualTo(id);
        assertThat(content.get(0).getMemberNickname()).isEqualTo(nickname);
        assertThat(content.get(0).getName()).isEqualTo(name);
        assertThat(content.get(0).getCategory()).isEqualTo(category);
        assertThat(content.get(0).getCreatedAt()).isEqualTo(createdAt);
        assertThat(content.get(0).isDeleted()).isEqualTo(false);
    }

    @Test
    @DisplayName("매장 상세 조회 성공")
    public void getRestaurantTest() {
        // given
        when(this.restaurantRepository.findById(any())).thenReturn(this.getOne());

        // when
        RestaurantDetailResponse restaurantDetailResponse = this.adminRestaurantService.getRestaurant(id);

        // then
        assertThat(restaurantDetailResponse.getId()).isEqualTo(id);
        assertThat(restaurantDetailResponse.getMemberId()).isEqualTo(memberId);
        assertThat(restaurantDetailResponse.getMemberNickname()).isEqualTo(nickname);
        assertThat(restaurantDetailResponse.getName()).isEqualTo(name);
        assertThat(restaurantDetailResponse.getCategory()).isEqualTo(category);
        assertThat(restaurantDetailResponse.getAddress()).isEqualTo(address);
        assertThat(restaurantDetailResponse.getLocation()).isEqualTo(location);
        assertThat(restaurantDetailResponse.getContact()).isEqualTo(contact);
        assertThat(restaurantDetailResponse.getMenu()).isEqualTo(menu);
        assertThat(restaurantDetailResponse.getTime()).isEqualTo(time);
        assertThat(restaurantDetailResponse.getProvision()).isEqualTo(provision);
        assertThat(restaurantDetailResponse.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("매장 상세 조회 실패 - 찾을 수 없는 매장")
    public void getRestaurantFailNotFoundTest() {
        // given
        when(this.restaurantRepository.findById(any())).thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.getRestaurant(id);
        });

        // then
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 등록 성공")
    public void createRestaurantTest() {
        // given
        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(memberId)
                .name(name)
                .category(category)
                .address(address)
                .location(location)
                .contact(contact)
                .menu(menu)
                .time(time)
                .provision(provision)
                .build();

        when(memberRepository.findById(memberId)).thenReturn(this.getMemberOne());
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RestaurantDetailResponse restaurantDetailResponse = adminRestaurantService.createRestaurant(restaurantRequest);

        // then
        assertThat(restaurantDetailResponse.getMemberId()).isEqualTo(memberId);
        assertThat(restaurantDetailResponse.getMemberNickname()).isEqualTo(nickname);
        assertThat(restaurantDetailResponse.getName()).isEqualTo(name);
        assertThat(restaurantDetailResponse.getCategory()).isEqualTo(category);
        assertThat(restaurantDetailResponse.getAddress()).isEqualTo(address);
        assertThat(restaurantDetailResponse.getLocation()).isEqualTo(location);
        assertThat(restaurantDetailResponse.getContact()).isEqualTo(contact);
        assertThat(restaurantDetailResponse.getMenu()).isEqualTo(menu);
        assertThat(restaurantDetailResponse.getTime()).isEqualTo(time);
        assertThat(restaurantDetailResponse.getProvision()).isEqualTo(provision);
    }

    @Test
    @DisplayName("매장 등록 실패 - 찾을 수 없는 회원")
    public void createRestaurantFailMemberNotFoundTest() {
        // given
        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(memberId)
                .name(name)
                .category(category)
                .address(address)
                .location(location)
                .contact(contact)
                .menu(menu)
                .time(time)
                .provision(provision)
                .build();

        when(this.memberRepository.findById(any())).thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.createRestaurant(restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.NOT_FOUND_MEMBER.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.NOT_FOUND_MEMBER.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 등록 실패 - 메뉴 데이터 validation 체크")
    public void createRestaurantFailMenuBadRequestTest() {
        // given
        Map<String, Object> wrongMenu = new LinkedHashMap<>();
        wrongMenu.put("비빔밥", "error data"); // key(String), value(Integer)
        wrongMenu.put("불고기", 12000);

        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(memberId)
                .name(name)
                .category(category)
                .address(address)
                .location(location)
                .contact(contact)
                .menu(wrongMenu)
                .time(time)
                .provision(provision)
                .build();

        when(this.memberRepository.findById(memberId)).thenReturn(this.getMemberOne());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.createRestaurant(restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 등록 실패 - 영업시간 데이터 validation 체크")
    public void createRestaurantFailTimeBadRequestTest() {
        // given
        Map<String, Object> wrongTime = new LinkedHashMap<>();

        wrongTime.put(TimeOption.HOLIDAY.toString(), "error data"); // key(String), value(Boolean)
        wrongTime.put(TimeOption.ETC.toString(), "명절 당일만 휴업"); // key(String), value(String)

        TimeData timeData = TimeData.builder()
                .businessStatus(true)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .breakBusinessStatus(true)
                .breakStartTime(LocalTime.of(15, 0))
                .breakEndTime(LocalTime.of(17, 0))
                .build();

        for (DayInfo value : DayInfo.values()) {
            wrongTime.put(value.toString(), timeData);
        }

        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(memberId)
                .name(name)
                .category(category)
                .address(address)
                .location(location)
                .contact(contact)
                .menu(menu)
                .time(wrongTime)
                .provision(provision)
                .build();

        when(this.memberRepository.findById(memberId)).thenReturn(this.getMemberOne());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.createRestaurant(restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 등록 실패 - 편의시설 데이터 validation 체크")
    public void createRestaurantFailProvisionBadRequestTest() {
        // given
        Map<String, Object> wrongProvision = new LinkedHashMap<>();

        for (Provision value : Provision.values()) {
            wrongProvision.put(value.toString(), "error data"); // key(String), value(Boolean)
        }

        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(memberId)
                .name(name)
                .category(category)
                .address(address)
                .location(location)
                .contact(contact)
                .menu(menu)
                .time(time)
                .provision(wrongProvision)
                .build();

        when(this.memberRepository.findById(memberId)).thenReturn(this.getMemberOne());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.createRestaurant(restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 수정 성공")
    public void modifyRestaurantTest() {
        // given
        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(modifyMemberId)
                .name(modifyName)
                .category(modifyCategory)
                .address(modifyAddress)
                .location(modifyLocation)
                .contact(modifyContact)
                .menu(modifyMenu)
                .time(modifyTime)
                .provision(modifyProvision)
                .build();

        when(memberRepository.findById(modifyMemberId)).thenReturn(this.getModifyMemberOne());
        when(restaurantRepository.findById(any())).thenReturn(this.getOne());
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RestaurantDetailResponse restaurantDetailResponse = adminRestaurantService.modifyRestaurant(id, restaurantRequest);

        // then
        assertThat(restaurantDetailResponse.getId()).isEqualTo(id);
        assertThat(restaurantDetailResponse.getMemberId()).isEqualTo(modifyMemberId);
        assertThat(restaurantDetailResponse.getMemberNickname()).isEqualTo(modifyNickname);
        assertThat(restaurantDetailResponse.getName()).isEqualTo(modifyName);
        assertThat(restaurantDetailResponse.getCategory()).isEqualTo(modifyCategory);
        assertThat(restaurantDetailResponse.getAddress()).isEqualTo(modifyAddress);
        assertThat(restaurantDetailResponse.getLocation()).isEqualTo(modifyLocation);
        assertThat(restaurantDetailResponse.getContact()).isEqualTo(modifyContact);
        assertThat(restaurantDetailResponse.getMenu()).isEqualTo(modifyMenu);
        assertThat(restaurantDetailResponse.getTime()).isEqualTo(modifyTime);
        assertThat(restaurantDetailResponse.getProvision()).isEqualTo(modifyProvision);
    }

    @Test
    @DisplayName("매장 수정 실패 - 찾을 수 없는 매장")
    public void modifyRestaurantFailNotFoundTest() {
        // given
        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(modifyMemberId)
                .name(modifyName)
                .category(modifyCategory)
                .address(modifyAddress)
                .location(modifyLocation)
                .contact(modifyContact)
                .menu(modifyMenu)
                .time(modifyTime)
                .provision(modifyProvision)
                .build();

        when(restaurantRepository.findById(any())).thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.modifyRestaurant(id, restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 수정 실패 - 찾을 수 없는 회원")
    public void modifyRestaurantFailMemberNotFoundTest() {
        // given
        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(modifyMemberId)
                .name(modifyName)
                .category(modifyCategory)
                .address(modifyAddress)
                .location(modifyLocation)
                .contact(modifyContact)
                .menu(modifyMenu)
                .time(modifyTime)
                .provision(modifyProvision)
                .build();

        when(restaurantRepository.findById(any())).thenReturn(this.getOne());
        when(memberRepository.findById(modifyMemberId)).thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.modifyRestaurant(id, restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.NOT_FOUND_MEMBER.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.NOT_FOUND_MEMBER.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 수정 실패 - 메뉴 데이터 validation 체크")
    public void modifyRestaurantFailMenuBadRequestTest() {
        // given
        Map<String, Object> wrongMenu = new LinkedHashMap<>();
        wrongMenu.put("비빔밥", "error data"); // key(String), value(Integer)
        wrongMenu.put("불고기", 12000);

        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(modifyMemberId)
                .name(modifyName)
                .category(modifyCategory)
                .address(modifyAddress)
                .location(modifyLocation)
                .contact(modifyContact)
                .menu(wrongMenu)
                .time(modifyTime)
                .provision(modifyProvision)
                .build();

        when(restaurantRepository.findById(any())).thenReturn(this.getOne());
        when(memberRepository.findById(modifyMemberId)).thenReturn(this.getModifyMemberOne());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.modifyRestaurant(id, restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 수정 실패 - 영업시간 데이터 validation 체크")
    public void modifyRestaurantFailTimeBadRequestTest() {
        // given
        Map<String, Object> wrongTime = new LinkedHashMap<>();

        wrongTime.put(TimeOption.HOLIDAY.toString(), "error data"); // key(String), value(Boolean)
        wrongTime.put(TimeOption.ETC.toString(), "명절 당일만 휴업"); // key(String), value(String)

        TimeData timeData = TimeData.builder()
                .businessStatus(true)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .breakBusinessStatus(true)
                .breakStartTime(LocalTime.of(15, 0))
                .breakEndTime(LocalTime.of(17, 0))
                .build();

        for (DayInfo value : DayInfo.values()) {
            wrongTime.put(value.toString(), timeData);
        }

        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(modifyMemberId)
                .name(modifyName)
                .category(modifyCategory)
                .address(modifyAddress)
                .location(modifyLocation)
                .contact(modifyContact)
                .menu(modifyMenu)
                .time(wrongTime)
                .provision(modifyProvision)
                .build();

        when(restaurantRepository.findById(any())).thenReturn(this.getOne());
        when(memberRepository.findById(modifyMemberId)).thenReturn(this.getModifyMemberOne());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.modifyRestaurant(id, restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 수정 실패 - 편의시설 데이터 validation 체크")
    public void modifyRestaurantFailProvisionBadRequestTest() {
        // given
        Map<String, Object> wrongProvision = new LinkedHashMap<>();

        for (Provision value : Provision.values()) {
            wrongProvision.put(value.toString(), "error data"); // key(String), value(Boolean)
        }

        RestaurantRequest restaurantRequest = RestaurantRequest.builder()
                .memberId(modifyMemberId)
                .name(modifyName)
                .category(modifyCategory)
                .address(modifyAddress)
                .location(modifyLocation)
                .contact(modifyContact)
                .menu(modifyMenu)
                .time(modifyTime)
                .provision(wrongProvision)
                .build();

        when(restaurantRepository.findById(any())).thenReturn(this.getOne());
        when(memberRepository.findById(modifyMemberId)).thenReturn(this.getModifyMemberOne());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.modifyRestaurant(id, restaurantRequest);
        });

        // then
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.INVALID_INPUT_VALUE.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 삭제 성공")
    public void deleteRestaurantTest() {
        // given
        Restaurant restaurant = getRestaurantData();
        List<RestaurantStock> restaurantStocks = getRestaurantStocksData();

        when(restaurantRepository.findById(id)).thenReturn(Optional.of(restaurant));
        when(restaurantStockRepository.findByRestaurantAndDeletedAtIsNull(restaurant)).thenReturn(restaurantStocks);

        ArgumentCaptor<Restaurant> restaurantCaptor = ArgumentCaptor.forClass(Restaurant.class);
        ArgumentCaptor<List<RestaurantStock>> restaurantStocksCaptor = ArgumentCaptor.forClass(List.class);

        // When
        adminRestaurantService.deleteRestaurant(id);

        // then
        verify(restaurantRepository).save(restaurantCaptor.capture());
        verify(restaurantStockRepository).saveAll(restaurantStocksCaptor.capture());

        Restaurant savedRestaurant = restaurantCaptor.getValue();
        List<RestaurantStock> savedRestaurantStocks = restaurantStocksCaptor.getValue();

        assertThat(savedRestaurant.getDeletedAt()).isNotNull();
        savedRestaurantStocks.forEach(restaurantStock -> {
            assertThat(restaurantStock.getDeletedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("매장 삭제 실패 - 찾을 수 없는 매장")
    public void deleteRestaurantFailNotFoundTest() {
        // given
        when(restaurantRepository.findById(any())).thenReturn(Optional.empty());

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.deleteRestaurant(id);
        });

        // then
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("매장 삭제 실패 - 이미 삭제된 매장")
    public void deleteRestaurantFailAlreadyDeletedTest() {
        // given
        Restaurant deletedRestaurant = this.getOne().get();
        deletedRestaurant = deletedRestaurant.toBuilder()
                .deletedAt(LocalDateTime.now())
                .build();

        when(restaurantRepository.findById(any())).thenReturn(Optional.of(deletedRestaurant));

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminRestaurantService.deleteRestaurant(id);
        });

        // then
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getStatus(), exception.getStatus());
        assertEquals(HttpResponse.Fail.NOT_FOUND_RESTAURANT.getMessage(), exception.getMessage());
    }

    private Page<Restaurant> getRestaurants() {
        List<Restaurant> list = List.of(this.getRestaurantData());
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<Restaurant>(list, pageable, list.size());
    }

    private Optional<Restaurant> getOne() {
        return Optional.of(this.getRestaurantData());
    }

    private Optional<Member> getMemberOne() {
        return Optional.of(this.getMemberData());
    }

    private Optional<Member> getModifyMemberOne() {
        return Optional.of(this.getModifyMemberData());
    }

    private Restaurant getRestaurantData() {
        Member member = getMemberData();

        return Restaurant.builder()
                .id(id)
                .members(member)
                .category(category)
                .name(name)
                .address(address)
                .location(location)
                .contact(contact)
                .menu(menu)
                .time(time)
                .provision(provision)
                .createdAt(createdAt)
                .build();
    }

    private Member getMemberData() {
        return Member.builder()
                .id(memberId)
                .email(email)
                .provider(ProviderType.ofProvider(provider))
                .name(memberName)
                .nickname(nickname)
                .role(MemberRole.ofRole(role))
                .phone(phone)
                .certifyAt(certifyAt)
                .agreedToServiceUse(agreedToServiceUse)
                .agreedToServicePolicy(agreedToServicePolicy)
                .agreedToServicePolicyUse(agreedToServicePolicyUse)
                .createdAt(memberCreatedAt)
                .build();
    }

    private Member getModifyMemberData() {
        return Member.builder()
                .id(modifyMemberId)
                .email(email)
                .provider(ProviderType.ofProvider(provider))
                .name(memberName)
                .nickname(modifyNickname)
                .role(MemberRole.ofRole(role))
                .phone(phone)
                .certifyAt(certifyAt)
                .agreedToServiceUse(agreedToServiceUse)
                .agreedToServicePolicy(agreedToServicePolicy)
                .agreedToServicePolicyUse(agreedToServicePolicyUse)
                .createdAt(memberCreatedAt)
                .build();
    }

    private List<RestaurantStock> getRestaurantStocksData() {
        Restaurant restaurant = getRestaurantData();

        return LongStream.rangeClosed(1, 2).mapToObj(i -> {
            Item item = Item.builder()
                    .name("itemName" + i)
                    .price(BigDecimal.valueOf(i))
                    .info("info")
                    .build();

            return RestaurantStock.builder()
                    .id(i)
                    .item(item)
                    .restaurant(restaurant)
                    .quantity(i)
                    .createdAt(createdAt)
                    .build();
        }).collect(Collectors.toList());
    }
}
