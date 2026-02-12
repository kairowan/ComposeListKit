package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.ComposeListKit

private data class CommentItem(
    val id: String,
    val author: String,
    val content: String,
    val time: String,
    val replies: List<CommentItem> = emptyList()
)

@Composable
fun CommentThreadScreen() {
    var comments by remember { mutableStateOf(sampleComments()) }
    var newCommentDraft by remember { mutableStateOf("") }
    var replyingToId by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember { mutableStateOf("") }
    var idSeed by remember { mutableIntStateOf(1000) }

    fun nextId(): String {
        val id = "c_local_$idSeed"
        idSeed += 1
        return id
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CommentComposer(
            value = newCommentDraft,
            hint = "写下你的评论",
            actionLabel = "发布",
            onValueChange = { newCommentDraft = it },
            onSubmit = {
                val text = newCommentDraft.trim()
                if (text.isNotEmpty()) {
                    comments = comments + CommentItem(
                        id = nextId(),
                        author = "Me",
                        content = text,
                        time = "刚刚"
                    )
                    newCommentDraft = ""
                }
            }
        )

        ComposeListKit<CommentItem> {
            data {
                list(items = comments, modifier = Modifier.weight(1f))
                key { it.id }
            }
            mode {
                commentThread(
                    childrenSelector = { it.replies },
                    maxDepth = 3,
                    initialExpandedDepth = 3,
                    itemContent = { comment, depth, hasChildren, expanded, onToggle ->
                        val canReply = depth < 2
                        val isReplying = replyingToId == comment.id

                        CommentRow(
                            comment = comment,
                            depth = depth,
                            hasChildren = hasChildren,
                            expanded = expanded,
                            canReply = canReply,
                            isReplying = isReplying,
                            replyDraft = replyDraft,
                            onReplyDraftChange = { replyDraft = it },
                            onRequestReply = {
                                if (replyingToId != comment.id) {
                                    replyDraft = ""
                                }
                                replyingToId = comment.id
                            },
                            onCancelReply = {
                                replyingToId = null
                                replyDraft = ""
                            },
                            onSubmitReply = {
                                val text = replyDraft.trim()
                                if (text.isNotEmpty()) {
                                    comments = appendReply(
                                        nodes = comments,
                                        parentId = comment.id,
                                        reply = CommentItem(
                                            id = nextId(),
                                            author = "Me",
                                            content = text,
                                            time = "刚刚"
                                        )
                                    )
                                    replyingToId = null
                                    replyDraft = ""
                                }
                            },
                            onToggleReplies = onToggle
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CommentComposer(
    value: String,
    hint: String,
    actionLabel: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(hint) },
                maxLines = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    enabled = value.isNotBlank(),
                    onClick = onSubmit
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    depth: Int,
    hasChildren: Boolean,
    expanded: Boolean,
    canReply: Boolean,
    isReplying: Boolean,
    replyDraft: String,
    onReplyDraftChange: (String) -> Unit,
    onRequestReply: () -> Unit,
    onCancelReply: () -> Unit,
    onSubmitReply: () -> Unit,
    onToggleReplies: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tonal = when (depth) {
        0 -> 2.dp
        1 -> 1.dp
        else -> 0.dp
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = tonal,
        color = if (depth == 0) colors.surfaceVariant.copy(alpha = 0.45f) else colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = comment.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canReply) {
                    Text(
                        text = "回复",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.clickable(onClick = onRequestReply)
                    )
                }
                if (hasChildren) {
                    Text(
                        text = if (expanded) "收起回复" else "展开 ${comment.replies.size} 条回复",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.clickable(onClick = onToggleReplies)
                    )
                }
            }

            if (isReplying) {
                OutlinedTextField(
                    value = replyDraft,
                    onValueChange = onReplyDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("回复 ${comment.author}") },
                    maxLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancelReply) {
                        Text("取消")
                    }
                    Button(
                        enabled = replyDraft.isNotBlank(),
                        onClick = onSubmitReply
                    ) {
                        Text("发送")
                    }
                }
            }
        }
    }
}

private fun appendReply(
    nodes: List<CommentItem>,
    parentId: String,
    reply: CommentItem
): List<CommentItem> {
    var appended = false

    fun update(node: CommentItem): CommentItem {
        if (node.id == parentId) {
            appended = true
            return node.copy(replies = node.replies + reply)
        }
        if (node.replies.isEmpty()) return node
        val nextReplies = node.replies.map(::update)
        return if (nextReplies == node.replies) node else node.copy(replies = nextReplies)
    }

    val updated = nodes.map(::update)
    return if (appended) updated else nodes
}

private fun sampleComments(): List<CommentItem> {
    return listOf(
        CommentItem(
            id = "c_1",
            author = "Alice",
            content = "这个 DSL 的结构比之前清晰很多，尤其是 mode 分块。",
            time = "2分钟前",
            replies = listOf(
                CommentItem(
                    id = "c_1_1",
                    author = "Bob",
                    content = "同意，而且现在分页和刷新的配置看起来更统一。",
                    time = "1分钟前",
                    replies = listOf(
                        CommentItem(
                            id = "c_1_1_1",
                            author = "Carol",
                            content = "如果再加评论线程模式就更完整了。",
                            time = "刚刚"
                        )
                    )
                ),
                CommentItem(
                    id = "c_1_2",
                    author = "David",
                    content = "建议默认展开两级，三级按需展开。",
                    time = "1分钟前"
                )
            )
        ),
        CommentItem(
            id = "c_2",
            author = "Eve",
            content = "二级评论现在在业务里很常见，三级主要是讨论串。",
            time = "5分钟前"
        )
    )
}
