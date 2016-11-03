package com.jakduk.api.restcontroller.board.vo;

import com.jakduk.core.model.simple.BoardFreeOfMinimum;
import com.jakduk.core.model.simple.BoardFreeSimple;
import com.jakduk.core.model.web.board.BoardFreeDetail;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author pyohwan
 *         16. 7. 16 오후 7:28
 */

@ApiModel(value = "자유게시판 글 상세")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FreePostOnDetailResponse {

    @ApiModelProperty(value = "글 상세")
    private BoardFreeDetail post;

    @ApiModelProperty(value = "앞 글")
    private BoardFreeOfMinimum prevPost;

    @ApiModelProperty(value = "뒷 글")
    private BoardFreeOfMinimum nextPost;

    @ApiModelProperty(value = "작성자의 최근 글")
    private List<BoardFreeSimple> latestPostsByWriter;
}
