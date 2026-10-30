package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.db.AppDatabase
import com.example.data.model.PromptEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Prompts are local notes: copying requires a user tap; no web injection or automatic send. */
@Composable
fun ManualPromptScreen(onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val dao = remember { AppDatabase.getDatabase(context, scope).promptDao() }
    val flow = remember(dao) { dao.getAllPromptsFlow() }
    val prompts by flow.collectAsState(initial = emptyList())
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("本地提示词（手动复制、手动粘贴）")
                Text("不会自动向 Arena 发送内容。请先检查剪贴板内容，粘贴后由您决定是否发送。")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("提示词") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Button(
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    onClick = {
                        val savedTitle = title.trim()
                        val savedContent = content.trim()
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    dao.insert(PromptEntity(
                                        title = savedTitle,
                                        content = savedContent,
                                        category = "手动",
                                        orderIndex = dao.count() + 1
                                    ))
                                }
                                title = ""
                                content = ""
                                onNotice("已保存在本机，未向 Arena 发送。")
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                onNotice("本地保存失败；不会发送到 Arena，也没有清除已有数据。")
                            }
                        }
                    }
                ) { Text("保存在本机") }
            }
        }
        if (prompts.isEmpty()) item { Text("暂无提示词。旧记录如有保存会出现在此处。") }
        items(prompts, key = { it.id }) { prompt ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(prompt.title)
                    Text(prompt.content)
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(prompt.content))
                        onNotice("已复制到剪贴板；请自行在 Agent 页面粘贴。")
                    }) { Text("复制文本") }
                }
            }
        }
    }
}
