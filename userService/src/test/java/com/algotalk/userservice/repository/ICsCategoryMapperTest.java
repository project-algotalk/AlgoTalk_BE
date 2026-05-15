package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.CsCategoryCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class ICsCategoryMapperTest {

    @Autowired
    private ICsCategoryMapper csCategoryMapper;

    @Test
    @DisplayName("CS_CATEGORY 조회 - 정렬 기준(CATEGORY_TYPE, PARENT_ID, DEPTH, SORT_ORDER) 확인")
    void getCategories_sortedAsExpected() throws Exception {
        List<CsCategoryCommand> categories = csCategoryMapper.getCsCategories();

        assertThat(categories).isNotNull();
        assertThat(categories).isNotEmpty();

        List<CsCategoryCommand> sorted = categories.stream()
                .sorted(
                        Comparator.comparing(CsCategoryCommand::getCategoryType, Comparator.nullsFirst(String::compareTo))
                                .thenComparing(CsCategoryCommand::getParentId, Comparator.nullsFirst(Long::compareTo))
                                .thenComparing(CsCategoryCommand::getSortOrder, Comparator.nullsFirst(Integer::compareTo))
                )
                .toList();

        assertThat(categories).containsExactlyElementsOf(sorted);
    }
}